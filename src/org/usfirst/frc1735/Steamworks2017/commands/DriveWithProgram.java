// RobotBuilder Version: 2.0
//
// This file was generated by RobotBuilder. It contains sections of
// code that are automatically generated and assigned by robotbuilder.
// These sections will be updated in the future when you export to
// Java from RobotBuilder. Do not put any code or make any change in
// the blocks indicating autogenerated code or it will be lost on an
// update. Deleting the comments indicating the section will prevent
// it from being updated in the future.


package org.usfirst.frc1735.Steamworks2017.commands;
import edu.wpi.first.wpilibj.command.Command;
import org.usfirst.frc1735.Steamworks2017.Robot;
import org.usfirst.frc1735.Steamworks2017.RobotMap;
import org.usfirst.frc1735.Steamworks2017.subsystems.DriveTrain;
import org.usfirst.frc1735.Steamworks2017.subsystems.DriveTrain.DrivetrainMode;

/**
 *
 */
public class DriveWithProgram extends Command {

    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=VARIABLE_DECLARATIONS
 
    // END AUTOGENERATED CODE,[ SOURCE=ROBOTBUILDER ID=VARIABLE_DECLARATIONS
	 // Member variables set by constructor
	private DrivetrainMode m_mode;
	private double m_driveMagDir;
	private double m_driveDist;
	private double m_turnAngle; // PID determines magnitude
	private double m_crabMagDir;
	private double m_crabDist;
	
	// Member variables set by initialize()
	private double m_FLStartRotation; // Front Left encoder starting value in rotation units
	private double m_FRStartRotation; // Front Right
	private double m_BLStartRotation; // Back Left
	private double m_BRStartRotation; // Back Right
	private double m_initialGyroAngle; // Starting Gyro value
	private double m_targetGyroAngle;  // Calculated target gyro angle

	// The full constructor with all the trimmings...
	// Units:  Time in seconds, distance in inches, angle in degrees.
    public DriveWithProgram(DriveTrain.DrivetrainMode mode,
    						double timeLimit,
    						double driveMagDir, double driveDist,
    						double crabMagDir,  double crabDist,
    						double turnAngle) { // PID determines magnitude

        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=VARIABLE_SETTING

        // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=VARIABLE_SETTING
        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=REQUIRES
        requires(Robot.driveTrain);

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=REQUIRES
        
        // Do the construction-time initialization
        m_mode = mode;
        setTimeout(timeLimit);
        m_driveMagDir = driveMagDir;
        m_driveDist = driveDist;
        m_turnAngle = turnAngle;
        m_crabMagDir = crabMagDir;
        m_crabDist = crabDist;
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    	// Sanity check the args.
    	// For instance, setting mode to traction and asking for Crab is not legal
    	if ((m_mode == DriveTrain.DrivetrainMode.kTraction) &&
    		(m_crabMagDir != 0)) {
    		System.err.println("Call to DriveWithProgram with mode=traction and nonzero Crab!  Ignoring crab value.");
    		m_crabMagDir = 0;
    	}
    	// Also, distance is assumed to be positive.  *DirMag determines direction
    	if (m_driveDist < 0) {
    		System.err.println("Negative driveDist provided.  Converting to positive.");
    		m_driveDist = -m_driveDist;
    	}
    	if (m_crabDist < 0) {
    		System.err.println("Negative crabDist provided.  Converting to positive.");
    		m_crabDist = -m_crabDist;
    	}
    	
    	// Get the initial values for all driveline encoders.  This value is in rotations.
    	m_FLStartRotation = -RobotMap.driveTrainFLMotor.getEncPosition()/2048.0;
    	m_FRStartRotation = RobotMap.driveTrainFRMotor.getEncPosition()/2048.0;
    	m_BLStartRotation = -RobotMap.driveTrainBLMotor.getEncPosition()/2048.0;
    	m_BRStartRotation = RobotMap.driveTrainBRMotor.getEncPosition()/2048.0;
    	
    	// Get the initial Gyro heading
    	m_initialGyroAngle = Robot.ahrs.getAngle();
    	System.out.println("Initial gyro angle = " + m_initialGyroAngle);

    	// Calculate our PID target for heading.
    	// If we are just driving straight, the turnAngle will be zero.  Maintain initial heading.
     	// If we are asked to turn, the turnAngle will be nonzero, and we will move to a new heading.
    	// Either way, the calculation of the target angle is the same:
    	m_targetGyroAngle = (m_initialGyroAngle + m_turnAngle);
    	System.out.println("Target gyro angle = " + m_initialGyroAngle);
   	
    	// Finally, enable the turn controller
    	Robot.driveTrain.drivelineController.setSetpoint(m_targetGyroAngle);	
    	Robot.driveTrain.drivelineController.enable();
   	
   }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
       	// Use mode bit to determine which driveline mode to use to accomplish the PID output reaction
    	if (m_mode == DriveTrain.DrivetrainMode.kMecanum) {
    		Robot.driveTrain.mecanumDrive(m_crabMagDir, m_driveMagDir, Robot.driveTrain.getPIDRotationRate()); // x,y,rot
    	}
    	else if (m_mode == DriveTrain.DrivetrainMode.kTraction) {
    		Robot.driveTrain.arcadeDrive(m_driveMagDir, Robot.driveTrain.getPIDRotationRate());// move, rot
    	}

    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
    	boolean timedOut = isTimedOut();
    	
        // Because we have multiple degrees of freedom that may be requested, we don't finish until ALL of them are achieved
    	// 1) Get state of drive limit
    	//    There are four encoders.  Claim victory if any of them reached the limit (Courtesy of the Department of Redundancy Department)
    	//encoder value is returned in units of rotations.
    	double FLCurrentRotation = -RobotMap.driveTrainFLMotor.getEncPosition()/2048.0;
    	double FRCurrentRotation = RobotMap.driveTrainFRMotor.getEncPosition()/2048.0;
    	double BLCurrentRotation = -RobotMap.driveTrainBLMotor.getEncPosition()/2048.0;
    	double BRCurrentRotation = RobotMap.driveTrainBRMotor.getEncPosition()/2048.0;

    	// Determine amount of travel in actual distance units
    	double FLDriveTravel = Math.abs(FLCurrentRotation - m_FLStartRotation) * DriveTrain.m_inchesPerRevolution;
    	double FRDriveTravel = Math.abs(FRCurrentRotation - m_FRStartRotation) * DriveTrain.m_inchesPerRevolution;
    	double BLDriveTravel = Math.abs(BLCurrentRotation - m_BLStartRotation) * DriveTrain.m_inchesPerRevolution;
    	double BRDriveTravel = Math.abs(BRCurrentRotation - m_BRStartRotation) * DriveTrain.m_inchesPerRevolution;
    	    	
    	// From travel, determine if we reached the drive distance limit on ANY encoder.
    	// We want some redundancy in case one encoder fails (i.e. wires get ripped out)
    	// but if one wheel slips on starup, it will reach the distance limit too early, and we won't have gone far enough
    	// Compromise:  Assume no more than one wheel slips, and make sure at least TWO got to the distance limit.
    	int FLDriveReached = (FLDriveTravel >= m_driveDist)?1:0; // Convert bool to int
    	int FRDriveReached = (FRDriveTravel >= m_driveDist)?1:0;
    	int BLDriveReached = (BLDriveTravel >= m_driveDist)?1:0;
    	int BRDriveReached = (BRDriveTravel >= m_driveDist)?1:0;
    	
    	boolean driveDistReached = (FLDriveReached + FRDriveReached + BLDriveReached + BRDriveReached) >= 2;
    	
    	// From travel, determine if we reached the crab distance limit
    	// This is tougher because wheels spin in opposite directions!
    	// Therefore the distance_per_revolution may be different.
    	// We SWAG that the crab distance will be sqrt(2)/2 compared to forward distance per rotation.
    	double FLCrabTravel = FLDriveTravel * Math.sqrt(2)/2;
    	double FRCrabTravel = FRDriveTravel * Math.sqrt(2)/2;
    	double BLCrabTravel = BLDriveTravel * Math.sqrt(2)/2;
    	double BRCrabTravel = BRDriveTravel * Math.sqrt(2)/2;

    	//@FIXME:  We need to work an equation for calculating crab distance based on the encoders, or forward motion will look like crab motion here.
    	// Simple crab-only may work here, but a mix of forward and crab needs some vector calculations performed...
    	int FLCrabReached = (FLCrabTravel >= m_crabDist)?1:0; // Convert bool to int
    	int FRCrabReached = (FRCrabTravel >= m_crabDist)?1:0;
    	int BLCrabReached = (BLCrabTravel >= m_crabDist)?1:0;
    	int BRCrabReached = (BRCrabTravel >= m_crabDist)?1:0;
    	
    	boolean crabDistReached = (FLCrabReached + FRCrabReached + BLCrabReached + BRCrabReached) >= 2;
    	
    	// IF we specified no angle, then the PID was trying to keep us on a constant heading an onTarget() is still valid to look at.
    	boolean angleReached = Robot.driveTrain.drivelineController.onTarget();
    			
    	return (timedOut || (driveDistReached && crabDistReached && angleReached));
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.driveTrain.drivelineController.disable(); // Stop the turn controller
    	Robot.driveTrain.stop(); // Stop the motors
    	
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}

// RobotBuilder Version: 2.0
//
// This file was generated by RobotBuilder. It contains sections of
// code that are automatically generated and assigned by robotbuilder.
// These sections will be updated in the future when you export to
// Java from RobotBuilder. Do not put any code or make any change in
// the blocks indicating autogenerated code or it will be lost on an
// update. Deleting the comments indicating the section will prevent
// it from being updated in the future.


package org.usfirst.frc1735.Steamworks2017.subsystems;

import org.usfirst.frc1735.Steamworks2017.Robot;
import org.usfirst.frc1735.Steamworks2017.RobotMap;
import org.usfirst.frc1735.Steamworks2017.commands.*;
import com.ctre.CANTalon;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


/**
 *
 */
public class DriveTrain extends Subsystem {

    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTANTS

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTANTS

    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DECLARATIONS
    private final CANTalon fLMotor = RobotMap.driveTrainFLMotor;
    private final CANTalon fRMotor = RobotMap.driveTrainFRMotor;
    private final CANTalon bLMotor = RobotMap.driveTrainBLMotor;
    private final CANTalon bRMotor = RobotMap.driveTrainBRMotor;
    private final RobotDrive robotDrive41 = RobotMap.driveTrainRobotDrive41;
    private final Compressor compressor = RobotMap.driveTrainCompressor;
    private final Solenoid solenoid = RobotMap.driveTrainSolenoid;

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DECLARATIONS


    // Put methods for controlling this subsystem
    // here. Call these from Commands.

    public void initDefaultCommand() {
        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DEFAULT_COMMAND

        setDefaultCommand(new DriveWIthJoysticks());

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DEFAULT_COMMAND

        // Set the default command for a subsystem here.
        // setDefaultCommand(new MySpecialCommand());
    }
    
    // Override constructor to initialize variables to defaults
    public DriveTrain() {
    	this.setTractionMode(); // Default to traction mode on startup.
    	m_isCrabExcursion = false; // We do not start out in any funny hybrid modes
    }
    
    public void arcadeDrive(double moveValue,double rotateValue) {
    	boolean squaredInputs = false; // Do not use decreased sensitivity at low speeds.
    	// Make sure we are in Traction mode:
    	this.setTractionMode();
    	robotDrive41.arcadeDrive(-moveValue, rotateValue, squaredInputs); //Asssume joystick inputs (Y fwd == -1)
    	printMXPInfo();
    	
    }
    
    public void mecanumDrive(double driveX,double driveY,double rotation) {
    	double gyroAngle = 0; // Do not use a gyro to implement field-oriented driving
    	// Make sure we are in Mecanum mode:
   		this.setMecanumMode();
   		
    	// We want to call this function for autonomous, and want the input assumptions to be non-joystick.
    	// (ie. forward motion is +1 value).
    	// Because the library for Mecanum (only) assumes joystick, we invert here (And the library inverts it back)
		robotDrive41.mecanumDrive_Cartesian(driveX, -driveY, rotation, gyroAngle ); // WPILIB ASSUMES Joystick input (Y forward == -1)
    	printMXPInfo();
    }

	public void octaCanumDriveWithJoysticks(Joystick joyLeft, Joystick joyRight) {
		// Extract the joystick values
		double joyLeftX, joyLeftY, joyRightX, joyRightY;
		
		// If an Xbox controller, try using the two sticks on controller 1 (Right side) instead of using two joysticks
		if (joyRight.getIsXbox()) {
			joyLeftX = joyRight.getRawAxis(0);  // Left stick X
			joyLeftY = joyRight.getRawAxis(1);  // Left stick Y
			joyRightX = joyRight.getRawAxis(4); // Right stick X
			joyRightY = joyRight.getRawAxis(5); // Right stick Y
		}
		else {
			joyLeftX  = joyLeft.getX();
			joyLeftY  = joyLeft.getY();
			joyRightX = joyRight.getX();
			joyRightY = joyRight.getY();
		}

		// Print the raw joystick inputs
		//System.out.println("joyLeftY="+joyLeftY+" joyLeftX="+joyLeftX + " joyRightY="+joyRightY+" joyRightX="+joyRightX);

		// Apply the 'dead zone' guardband to the joystick inputs:
		// Centered joysticks may not actually read as zero due to spring variances.
		// Therfore, remove any small values as being "noise".
		if (Math.abs(joyLeftX) < Robot.m_joystickFilter)
			joyLeftX = 0;
		if (Math.abs(joyLeftY) < Robot.m_joystickFilter)
			joyLeftY = 0;
		if (Math.abs(joyRightX) < Robot.m_joystickFilter)
			joyRightX = 0;
		if (Math.abs(joyRightY) < Robot.m_joystickFilter)
			joyRightY = 0;
		
		
		// Find out which operating mode is requested
		if (false) { // original, simple mode
		if (!this.isInMecanumMode() && (joyLeftX == 0)) {
			// Here, we are in arcade mode, and are not using the crab joystick.   Use the traction wheels.
			this.arcadeDrive(-joyRightY, joyRightX); // fwd/rvs, rotation (CW/right is negative)
		}
		else {
			// Drive with the mecanum wheels...
			this.mecanumDrive(joyLeftX, -joyRightY, joyRightX); // X motion (crab), Y motion (fwd/rvs), rotation
		}
		}
		else {
			// Complicated state machine.  When using the left joystick to crab, treat it as a temporary excursion into mecanum.
			// Must return to traction when stick is released, but this requires knowing whether we were in mecanum or traction before the crab operation.
			// (Call this isCrabExcursion; if true then we are in this funny state.
			// This operation is basically a 5-input state machine.
			// We need the force_mecanum, toggle, crab, mecanum, and joyleft inputs
			// next-state indicators for the four possible transitions (not encoded)
			boolean normal    = false;
			boolean enterCrab = false;
			boolean exitCrab  = false;
			boolean stayCrab  = false;
			
			// simplified input variable
			boolean isCrabbing = (joyLeftX != 0);
			
			// Transition table (illegal states and don't-cares are not listed, of course)
			if      (!m_isCrabExcursion && !isInMecanumMode() && !isCrabbing) //000
				normal = true;
			else if (!m_isCrabExcursion && !isInMecanumMode() &&  isCrabbing) //001
				enterCrab = true;
			else if (!m_isCrabExcursion &&  isInMecanumMode()               ) //010 or 011
				normal = true;			
			else if ( m_isCrabExcursion &&  isInMecanumMode() && !isCrabbing) //110
				exitCrab = true;
			else if ( m_isCrabExcursion &&  isInMecanumMode() &&  isCrabbing) //111
				stayCrab = true;
			else //when all else fails, behave normally as if there is no crab excursion mode!
				normal = true;  
			
			// There are other factors too-- do these just to ensure a sane state
			// if we decide to toggle while in a crab excursion, clear the crab bit. (done in this.toggleDriveTrain())
			// if we decide to force mecanum while in a crab excursion, clear the crab bit. (Done in the ForceMecanum command)
			
			// Now that we know the next state, execute on it:
			if (normal) {
				if (!this.isInMecanumMode() && (joyLeftX == 0)) {
					// Here, we are in arcade mode, and are not using the crab joystick.   Use the traction wheels.
					this.arcadeDrive(-joyRightY, joyRightX); // fwd/rvs, rotation (CW/right is negative)
				}
				else {
					// Drive with the mecanum wheels...
					this.mecanumDrive(joyLeftX, -joyRightY, joyRightX); // X motion (crab), Y motion (fwd/rvs), rotation
				}
			}
			else if (enterCrab) {
				// set the crab bit, and call the mecanum drive
				m_isCrabExcursion = true;
				this.mecanumDrive(joyLeftX, -joyRightY, joyRightX); // X motion (crab), Y motion (fwd/rvs), rotation
			}
			else if (exitCrab) {
				// Clear the crab bit, and call the traction drive
				m_isCrabExcursion = false;
				this.arcadeDrive(-joyRightY, joyRightX); // fwd/rvs, rotation (CW/right is negative)				
			}
			else if (stayCrab) {
				// continue to drive in mecanum mode
				this.mecanumDrive(joyLeftX, -joyRightY, joyRightX); // X motion (crab), Y motion (fwd/rvs), rotation
			}
			SmartDashboard.putBoolean("CrabExcursion", m_isCrabExcursion);

		}

			
			
	}
	
    // Function to STOP the drivetrain:
    public void stop() {
    	// Call the WPILIB code directly so we don't change modes when stopping...
    	robotDrive41.arcadeDrive(0, 0, false); //move, rotate, squaredInputs
    }
    
    public boolean isInMecanumMode() {
    	// Simply return the state of the global variable
    	return this.m_isInMecanumMode;
    }
    
    // All the things needed to enter mecanum mode
    public void setMecanumMode() {
    	// Set the global variable
    	this.m_isInMecanumMode = true;
    	
    	// Set the piston state
		this.solenoid.set(false); // release the pneumatics; gravity + "springs" will drop us back onto default mecanum wheels.
    	
    	// Set the correct motor inversion
		robotDrive41.setInvertedMotor(RobotDrive.MotorType.kFrontLeft, true);
		robotDrive41.setInvertedMotor(RobotDrive.MotorType.kRearLeft, true);
    	
		// Set the dashboard indicator
		SmartDashboard.putString("Drivetrain Mode", this.m_isInMecanumMode?"MECANUM":"TRACTION");	
    }
    
    // All the things needed to enter mecanum mode
    public void setTractionMode() {
    	// Set the global variable
    	this.m_isInMecanumMode = false;
    	
    	// Set the piston state
		this.solenoid.set(true); // engage the pneumatics to force the traction wheels down
    	
    	// Set the correct motor inversion
		robotDrive41.setInvertedMotor(RobotDrive.MotorType.kFrontLeft, false);
		robotDrive41.setInvertedMotor(RobotDrive.MotorType.kRearLeft, false);
    	
		// Set the dashboard indicator
		SmartDashboard.putString("Drivetrain Mode", this.m_isInMecanumMode?"MECANUM":"TRACTION");	
    }
    
    public void toggleDrivetrain() {
    	// Toggle the state of the driveline mode
		if (Robot.driveTrain.isInMecanumMode())
			Robot.driveTrain.setTractionMode();
		else // Must be in traction mode, so...
			Robot.driveTrain.setMecanumMode();
		// Clear any temporary crab excursion state
		clearCrabExcursion();
    }
    
    // For Gyro information shared by multiple subsystems
    // (Perhaps move to separate subsystem?)
    public void printMXPInfo() {
    	AHRS ahrs = Robot.ahrs; // this creates a local variable whose scope overrides the one in Robot; makes cut and paste from MXP examples easier
        /* These functions are compatible w/the WPI Gyro Class, providing a simple  */
        /* path for upgrading from the Kit-of-Parts gyro to the navx MXP            */
        
        SmartDashboard.putNumber(   "IMU_TotalYaw",         ahrs.getAngle()); //cumulative over time
        SmartDashboard.putNumber(   "IMU_YawRateDPS",       ahrs.getRate());
 
        /* Display 9-axis Heading (requires magnetometer calibration to be useful)  */
        SmartDashboard.putNumber(   "IMU_FusedHeading",     ahrs.getFusedHeading());

        /* Display estimates of velocity/displacement.  Note that these values are  */
        /* not expected to be accurate enough for estimating robot position on a    */
        /* FIRST FRC Robotics Field, due to accelerometer noise and the compounding */
        /* of these errors due to single (velocity) integration and especially      */
        /* double (displacement) integration.                                       */
        
        SmartDashboard.putNumber(   "Velocity_X",           ahrs.getVelocityX());
        SmartDashboard.putNumber(   "Velocity_Y",           ahrs.getVelocityY());
        SmartDashboard.putNumber(   "Displacement_X",       ahrs.getDisplacementX());
        SmartDashboard.putNumber(   "Displacement_Y",       ahrs.getDisplacementY());

    }
    
    // Used by external classes to cancel temporary excursions into crab mode (eg. forceMecanum and Toggle)
    public void clearCrabExcursion() {
    	m_isCrabExcursion = false;
    }

    // Member Variables
    boolean m_isInMecanumMode; // True = mecanum; false = traction
    boolean m_isCrabExcursion; // Keep track of whether we are temporarily crabbing from traction mode
}


package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;

public class Robot2011 extends IterativeRobot {
    private RobotDrive drive;
    private Joystick joystick1;
    private Joystick joystick2;

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        this.joystick1 = new Joystick(1);
        this.joystick2 = new Joystick(2);
        this.drive = new RobotDrive(
                new CANJaguar(2),  // front left
                new CANJaguar(3),  // rear left
                new CANJaguar(4),  // front right
                new CANJaguar(5)); // rear right
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
        this.drive.tankDrive(this.joystick1, this.joystick2);
    }
}
package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;

public class Robot2011 extends IterativeRobot {
    private DigitalInput input;
    private RobotDrive drive;
    private Joystick joystick1;
    private Joystick joystick2;

    public void robotInit() {
        System.out.println("Beginning robot initialization");

        this.input = new DigitalInput(1);
        this.drive = new RobotDrive(2, 3, 4, 5);
        this.joystick1 = new Joystick(1);
        this.joystick2 = new Joystick(2);

        System.out.println("Robot initialized");
    }

    public void disabledPeriodic() {
    }

    public void autonomousPeriodic() {
    }

    public void teleopPeriodic() {
        this.drive.tankDrive(this.joystick1, this.joystick2);
    }
}
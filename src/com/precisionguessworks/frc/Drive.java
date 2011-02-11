package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;

public class Drive {
    private static final double kInputDapen = 1.0;

    private final RobotDrive drive;
    private final Solenoid shifterSolenoid;

    private int inverseFactor = 1;
    
    public Drive(
            SpeedController frontLeftMotor,
            SpeedController rearLeftMotor,
            SpeedController frontRightMotor,
            SpeedController rearRightMotor,
            Solenoid shifterSolenoid) {
        this.drive = new RobotDrive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor);
        this.shifterSolenoid = shifterSolenoid;
    }

    public void invert() {
        this.inverseFactor = -this.inverseFactor;
    }

    public void shiftUp() {
        this.shifterSolenoid.set(true);
    }

    public void shiftDown() {
        this.shifterSolenoid.set(false);
    }

    public void tankDrive(double left, double right) {
        this.drive.tankDrive(left, -right);
    }

    public void tankDrive(final GenericHID leftJoystick, final GenericHID rightJoystick) {
        double left = leftJoystick.getY() * kInputDapen;
        double right = rightJoystick.getY() * kInputDapen;

        this.tankDrive(left, right);
    }

    public void tankDrive(final LogitechDualActionGamepad gamepad) {
        double left = gamepad.getLeftY() * kInputDapen;
        double right = gamepad.getRightY() * kInputDapen;

        this.tankDrive(left, right);
    }
}
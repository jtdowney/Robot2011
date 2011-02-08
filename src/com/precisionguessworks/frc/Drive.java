package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;

public class Drive {
    private static final double InputDapen = 0.5;

    private final SpeedController frontLeftMotor;
    private final SpeedController rearLeftMotor;
    private final SpeedController frontRightMotor;
    private final SpeedController rearRightMotor;
    private final Solenoid shifterSolenoid;

    private int inverseFactor = 1;
    
    public Drive(
            SpeedController frontLeftMotor,
            SpeedController rearLeftMotor,
            SpeedController frontRightMotor,
            SpeedController rearRightMotor,
            Solenoid shifterSolenoid) {
        this.frontLeftMotor = frontLeftMotor;
        this.rearLeftMotor = rearLeftMotor;
        this.frontRightMotor = frontRightMotor;
        this.rearRightMotor = rearRightMotor;
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
        this.frontLeftMotor.set(left * this.inverseFactor);
        this.rearLeftMotor.set(left * this.inverseFactor);
        this.frontRightMotor.set(right * this.inverseFactor);
        this.rearRightMotor.set(right * this.inverseFactor);
    }

    public void tankDrive(GenericHID left, GenericHID right) {
        this.frontLeftMotor.set(left.getX() * InputDapen * this.inverseFactor);
        this.rearLeftMotor.set(left.getX() * InputDapen * this.inverseFactor);
        this.frontRightMotor.set(right.getX() * InputDapen * this.inverseFactor);
        this.rearRightMotor.set(right.getX() * InputDapen * this.inverseFactor);
    }
}
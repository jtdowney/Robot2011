package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Drive {
    private static final double kInputDampen = 1.0;

    private int inverseFactor = 1;
    private final CANJaguar frontLeftMotor;
    private final CANJaguar rearLeftMotor;
    private final CANJaguar frontRightMotor;
    private final CANJaguar rearRightMotor;
    private final Solenoid shiftUpSolenoid;
    private final Solenoid shiftDownSolenoid;
    
    public Drive(
            CANJaguar frontLeftMotor,
            CANJaguar rearLeftMotor,
            CANJaguar frontRightMotor,
            CANJaguar rearRightMotor,
            Solenoid shiftUpSolenoid,
            Solenoid shiftDownSolenoid) {
        this.shiftUpSolenoid = shiftUpSolenoid;
        this.shiftDownSolenoid = shiftDownSolenoid;
        this.frontLeftMotor = frontLeftMotor;
        this.rearLeftMotor = rearLeftMotor;
        this.frontRightMotor = frontRightMotor;
        this.rearRightMotor = rearRightMotor;
    }

    public Drive(
            CANJaguar leftMotor,
            CANJaguar rightMotor,
            Solenoid shiftUpSolenoid,
            Solenoid shiftDownSolenoid)
    {
        this(leftMotor, leftMotor, rightMotor,
                rightMotor, shiftUpSolenoid, shiftDownSolenoid);
    }

    public void invert() {
        this.inverseFactor = -this.inverseFactor;
    }

    public void shiftUp() {
        this.shiftUpSolenoid.set(true);
        this.shiftDownSolenoid.set(false);
    }

    public void shiftDown() {
        this.shiftUpSolenoid.set(false);
        this.shiftDownSolenoid.set(true);
    }

    public void shiftHold() {
        this.shiftUpSolenoid.set(false);
        this.shiftDownSolenoid.set(false);
    }

    public void tankDrive(double left, double right) {
        left = Drive.limit(left);
        right = -Drive.limit(right);

        byte syncGroup = (byte) 128;

        try {
            this.frontLeftMotor.setX(left, syncGroup);
            this.rearLeftMotor.setX(left, syncGroup);
            this.frontRightMotor.setX(right, syncGroup);
            this.rearRightMotor.setX(right, syncGroup);
            
            CANJaguar.updateSyncGroup(syncGroup);
        } catch (CANTimeoutException ex) {
        }
    }

    protected static double limit(double num) {
        if (num > 1.0) {
            return 1.0;
        }
        if (num < -1.0) {
            return -1.0;
        }
        
        return num;
    }
}
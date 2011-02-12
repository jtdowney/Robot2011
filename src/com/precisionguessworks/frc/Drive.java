package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Drive {
    private static final double kInputDapen = 1.0;

    private int inverseFactor = 1;
    private final CANJaguar frontLeftMotor;
    private final CANJaguar rearLeftMotor;
    private final CANJaguar frontRightMotor;
    private final CANJaguar rearRightMotor;
    private final Solenoid shifterSolenoid;
    
    public Drive(
            CANJaguar frontLeftMotor,
            CANJaguar rearLeftMotor,
            CANJaguar frontRightMotor,
            CANJaguar rearRightMotor,
            Solenoid shifterSolenoid) {
        this.shifterSolenoid = shifterSolenoid;
        this.frontLeftMotor = frontLeftMotor;
        this.rearLeftMotor = rearLeftMotor;
        this.frontRightMotor = frontRightMotor;
        this.rearRightMotor = rearRightMotor;
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
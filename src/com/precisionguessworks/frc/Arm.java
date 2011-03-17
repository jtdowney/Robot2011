package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.AnalogChannel;
import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Arm {
    public static final double kPUp = 0.0043;
    //public static final double kIUp = 0.0001;
    public static final double kIUp = 0.0008;
    public static final double kDUp = 0.003;

    public static final double kPDown = 0.002;
    //public static final double kIDown = 0.00018;
    public static final double kIDown = 0.0008;
    public static final double kDDown = 0.003;

    private final AnalogChannel armPotentiometer;
    private final ArmOutput armOutput;
    private final PIDController controller;

    private int lastPosition = 0;

    public Arm(AnalogChannel armPotentiometer, CANJaguar topMotor, CANJaguar bottomMotor) {
        this.armPotentiometer = armPotentiometer;
        this.armOutput = new ArmOutput(topMotor, bottomMotor);
        this.controller = new PIDController(kPUp, kIUp, kDUp, armPotentiometer, armOutput);
        this.controller.setOutputRange(-0.35, 0.35);
        this.controller.setInputRange(200, 750);

        this.controller.enable();
    }
    
    public int getCurrentPosition() {
        return this.armPotentiometer.getValue();
    }

    public int getTargetPosition() {
        return (int) this.controller.getSetpoint();
    }

    public void holdPosition() {
        this.setPosition(this.getCurrentPosition());
    }

    public void setPosition(int position) {
        this.lastPosition = ((int)this.controller.getSetpoint());
        this.controller.setSetpoint(position);
        if (this.lastPosition > position)
        {
            // Going DOWN
            this.controller.setPID(kPDown, kIDown, kDDown);
            this.controller.setOutputRange(-0.3, 0.3);
        }
        else
        {
            // Going UP
            this.controller.setPID(kPUp, kIUp, kDUp);
            this.controller.setOutputRange(-0.35, 0.35);
        }
    }

    public double getCurrentSpeed() {
        return this.armOutput.getCurrentSpeed();
    }

    public void resetSpeedLimiter() {
        this.armOutput.resetPrevSpeed();
    }

    protected void resetPIDController() {
        this.controller.reset();
        this.holdPosition();
        this.controller.enable();
    }

    protected void resetPIDInternals() {
        this.controller.reset();
        this.controller.enable();
    }

    public void manualDrive(double output) {
        this.armOutput.drive(output);
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

    public class ArmOutput implements PIDOutput {
        public static final double maxDelta = .05;

        private CANJaguar topMotor;
        private CANJaguar bottomMotor;
        private double prevSpeed = 0;

        public ArmOutput(CANJaguar topMotor, CANJaguar bottomMotor) {
            this.topMotor = topMotor;
            this.bottomMotor = bottomMotor;
        }

        public void resetPrevSpeed()
        {
            this.prevSpeed = 0;
        }

        public double getCurrentSpeed() {
            return this.prevSpeed;
        }

        public void drive(double output) {
            byte syncGroup = (byte) 64;

            if (output > prevSpeed && Math.abs(output - prevSpeed) > maxDelta)
            {
                output = prevSpeed + maxDelta;
            }
            else if (output < prevSpeed && Math.abs(prevSpeed - output) > maxDelta)
            {
                output = prevSpeed - maxDelta;
            }

            double top = Arm.limit(output);
            double bottom = -Arm.limit(output);
            this.prevSpeed = output;

            try {
                this.topMotor.setX(top, syncGroup);
                this.bottomMotor.setX(bottom, syncGroup);

                CANJaguar.updateSyncGroup(syncGroup);
            } catch (CANTimeoutException ex) {
            }
        }

        public void pidWrite(double output) {
            this.drive(output);
        }
    }
}
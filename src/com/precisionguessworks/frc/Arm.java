package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.AnalogChannel;
import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Arm {
    public static final int kBaseValue = 200;
    
    public static final double kPUp = 0.004;
    public static final double kIUp = 0.00007;
    public static final double kDUp = .00002;

    public static final double kPDown = 0.001;
    public static final double kIDown = .000013;
    public static final double kDDown = .00002;

    public static final double kPStable = 0.006;
    public static final double kIStable = 0.000075;
    public static final double kDStable = .000022;

    public static final int kMaxSetpointDelta = 10;      // units of (points*10^2)/sec

    private final AnalogChannel armPotentiometer;
    private final ArmOutput armOutput;
    private final PIDController controller;

    private int lastPosition = 0;
    private int targetPosition = 0;
    private int position = 0;
    private int prevPosition = 0;

    public Arm(AnalogChannel armPotentiometer, CANJaguar topMotor, CANJaguar bottomMotor) {
        this.armPotentiometer = armPotentiometer;
        this.armOutput = new ArmOutput(topMotor, bottomMotor);
        this.controller = new PIDController(kPUp, kIUp, kDUp, armPotentiometer, armOutput);
        this.controller.setOutputRange(-0.5, 0.5);
        this.controller.setInputRange(150, 900);

        this.controller.enable();
        this.targetPosition = this.getCurrentPosition();
    }
    
    public int getCurrentPosition() {
        return this.armPotentiometer.getValue();
    }

    public int getTargetPosition() {
        return (int) this.targetPosition;
    }

    public int getSetpoint() {
        return (int) this.controller.getSetpoint();
    }

    public void holdPosition() {
        System.out.println("hold position: " + this.getCurrentPosition());
        this.setRawPosition(this.getCurrentPosition());
    }

    public void schedule() {
        position = this.getTargetPosition();
        lastPosition = this.getRawPosition();

        // If we aren't moving and not too far off, bump up the power!
        if(Math.abs(prevPosition - lastPosition) < 3 && Math.abs(lastPosition - position)  < 40) {
            if(lastPosition > 500) {
                this.controller.setPID(kPStable / 3, kIStable, kDStable);
            }
            else {
                this.controller.setPID(kPStable, kIStable, kDStable);
            }
            this.controller.setOutputRange(-0.5, .5);
//            System.out.println("using stable pid.");
        }
        else if (position > lastPosition) {
            //GOING UP
            this.controller.setPID(kPUp, kIUp, kDUp);
            this.controller.setOutputRange(-0.5, .5);
//            System.out.println("up pid");
        }
        else {
            //GOING DOWN
            this.controller.setPID(kPDown, kIDown, kDDown);
            this.controller.setOutputRange(-0.45, 0.45);
//            System.out.println("down pid");
        }
//        System.out.println("scheduler: " + position + ", " + lastPosition);

        if(position - lastPosition > 0) {
            // INCREASING
            if(position - lastPosition > kMaxSetpointDelta) {
//                System.out.println("Would be scheduled to " + (lastPosition + kMaxSetpointDelta));
//                System.out.println("   instead of: " + position);
                position = lastPosition + kMaxSetpointDelta;
            }
        }
        else if (lastPosition - position > 0) {
            // DECREASING
            if(lastPosition - position > kMaxSetpointDelta) {
//                System.out.println("Would be scheduled to " + (lastPosition - kMaxSetpointDelta));
//                System.out.println("   instead of: " + position);
                position = lastPosition - kMaxSetpointDelta;
            }
        }
        this.setRawPosition(position);
        prevPosition = lastPosition;
    }

    private int time;

    public void setRawPosition(int position) {
        this.controller.setSetpoint(position);
    }

    public int getRawPosition() {
        return (int)this.controller.getSetpoint();
    }

    public void setPosition(int position) {
        int newPosition = kBaseValue + position;
        
        
//        System.out.println("target position: " + newPosition);

        if (this.targetPosition > newPosition)
        {
            // Going DOWN
            this.controller.setPID(kPDown, kIDown, kDDown);
            this.controller.setOutputRange(-0.45, 0.45);
            System.out.println("down pid");
        }
        else
        {
            // Going UP
            this.controller.setPID(kPUp, kIUp, kDUp);
            this.controller.setOutputRange(-0.5, .5);
            System.out.println("up pid");
        }
        this.targetPosition = newPosition;
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

    public class ArmPID extends PIDController {
        public ArmPID(double kP, double kI, double kD, PIDSource pidSource, PIDOutput pidOutput) {
            super(kP, kI, kD, pidSource, pidOutput);
        }

//        public double getTotalError() {
//            return this.m_totalError;
//        }
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
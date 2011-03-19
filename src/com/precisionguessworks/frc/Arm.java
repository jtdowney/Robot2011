package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.AnalogChannel;
import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Arm {
    public static final double kPUp = 0.004;
    public static final double kIUp = 0.00006;
    public static final double kDUp = .00002;

    public static final double kPDown = 0.001;
    public static final double kIDown = .00001;
    public static final double kDDown = .00002;

    public static final int kMaxSetpointDelta = 3;      // units of (points*10^2)/sec

    private final AnalogChannel armPotentiometer;
    private final ArmOutput armOutput;
    private final PIDController controller;

    private int lastPosition = 0;
    private Timer scheduler;

    public Arm(AnalogChannel armPotentiometer, CANJaguar topMotor, CANJaguar bottomMotor) {
        this.armPotentiometer = armPotentiometer;
        this.armOutput = new ArmOutput(topMotor, bottomMotor);
        this.controller = new PIDController(kPUp, kIUp, kDUp, armPotentiometer, armOutput);
        this.controller.setOutputRange(-0.5, 0.5);
        this.controller.setInputRange(150, 900);

        this.controller.enable();
        scheduler = new Timer();
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

    private int time;
    public void setPosition(int position) {
        this.lastPosition = ((int)this.controller.getSetpoint());

//        //Setpoint scheduling
//        time = (int)(scheduler.get() * 100);
//        if(time > 0) {
//            if(position - lastPosition > 0) {
//                // INCREASING
//                if(position - lastPosition > kMaxSetpointDelta / time) {
//                    System.out.println("Would be scheduled to " + (lastPosition + kMaxSetpointDelta));
//                    System.out.println("   instead of: " + position);
////                    position = lastPosition + kMaxSetpointDelta;
//                }
//            }
//            else if (lastPosition - position > 0) {
//                // DECREASING
//                if(lastPosition - position > kMaxSetpointDelta / time) {
//                    System.out.println("Would be scheduled to " + (lastPosition - kMaxSetpointDelta));
//                    System.out.println("   instead of: " + position);
////                    position = lastPosition - kMaxSetpointDelta;
//                }
//            }
//        }
//
//        scheduler.reset();
//        scheduler.start();

        this.controller.setSetpoint(position);
        if (this.lastPosition > position)
        {
            // Going DOWN
            this.controller.setPID(kPDown, kIDown, kDDown);
            this.controller.setOutputRange(-0.45, 0.45);
        }
        else
        {
            // Going UP
            this.controller.setPID(kPUp, kIUp, kDUp);
            this.controller.setOutputRange(-0.5, .5);
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
package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Arm {
    public static final double kPUp = 0.004;
    public static final double kIUp = 0.000085;
    public static final double kDUp = 0.003;

    public static final double kPDown = 0.0015;
    public static final double kIDown = 0.000085;
    public static final double kDDown = 0.003;

    public static double kP = 0;
    public static double kI = 0;
    public static double kD = 0;

    private final ArmOutput armOutput;
    private final PIDController controller;
    private final Tower tower;

    private int lastPosition = 0;

    public Arm(PIDSource armPotentiometer, CANJaguar topMotor, CANJaguar bottomMotor, Tower tower) {
        this.armOutput = new ArmOutput(topMotor, bottomMotor);
        this.controller = new PIDController(kPUp, kIUp, kDUp, armPotentiometer, armOutput);
        this.controller.setOutputRange(-0.4, 0.4);
        this.controller.setInputRange(120, 750);

        this.tower = tower;
    }

    public void setPosition(int position) {
        this.lastPosition = ((int)this.controller.getSetpoint());
        this.controller.setSetpoint(position);
        if (this.lastPosition > position)
        {
            // Going DOWN
            this.controller.setPID(kPDown, kIDown, kDDown);
            Arm.kP = kPUp;
            Arm.kI = kIUp;
            Arm.kD = kDUp;
        }
        else
        {
            // Going UP
            this.controller.setPID(kPUp, kIUp, kDUp);
            Arm.kP = kPDown;
            Arm.kI = kIDown;
            Arm.kD = kDDown;
        }
        this.controller.enable();
    }

    public void resetSpeedLimiter()
    {
        this.armOutput.resetPrevSpeed();
    }

    public void setFloorPosition() {
        this.tower.lower();
        this.setPosition(0);
    }

    public void gotoProtectedPosition() {
        this.tower.raise();
        this.setPosition(100);
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
        public static final double maxDelta = .04;

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

        public void drive(double output) {
            byte syncGroup = (byte) 64;
//            System.out.print("raw out: " + output);
            if (output > prevSpeed && Math.abs(output - prevSpeed) > maxDelta)
            {
                output = prevSpeed + maxDelta;
            }
            else if (output < prevSpeed && Math.abs(prevSpeed - output) > maxDelta)
            {
                output = prevSpeed - maxDelta;
            }
//            System.out.println(", limited out: " + output);
            double left = Arm.limit(output);
            double right = -Arm.limit(output);
            prevSpeed = output;

            try {
                this.topMotor.setX(left, syncGroup);
                this.bottomMotor.setX(right, syncGroup);

                CANJaguar.updateSyncGroup(syncGroup);
            } catch (CANTimeoutException ex) {
            }
        }

        public void pidWrite(double output) {
            this.drive(output);
        }
    }
}
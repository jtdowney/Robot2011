package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Arm {
    private static final double P = 1.0;
    private static final double I = 0.0;
    private static final double D = 0.0;

    private final ArmOutput armOutput;
    private final PIDController controller;
    private final Tower tower;

    public Arm(PIDSource armPotentiometer, CANJaguar topMotor, CANJaguar bottomMotor, Tower tower) {
        this.armOutput = new ArmOutput(topMotor, bottomMotor);
        this.controller = new PIDController(P, I, D, armPotentiometer, armOutput);
        this.tower = tower;
    }

    public void setPosition(int position) {
        this.controller.setSetpoint(position);
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
        private CANJaguar topMotor;
        private CANJaguar bottomMotor;

        public ArmOutput(CANJaguar topMotor, CANJaguar bottomMotor) {
            this.topMotor = topMotor;
            this.bottomMotor = bottomMotor;
        }

        public void drive(double output) {
            byte syncGroup = (byte) 64;

            double left = Arm.limit(output);
            double right = -Arm.limit(output);

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
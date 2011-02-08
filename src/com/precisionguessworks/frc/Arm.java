package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.SpeedController;

public class Arm {
    private static final double P = 1.0;
    private static final double I = 0.0;
    private static final double D = 0.0;

    private final PIDController controller;
    private final Tower tower;

    public Arm(PIDSource armPotentiometer, SpeedController topMotor, SpeedController bottomMotor, Tower tower) {
        this.controller = new PIDController(P, I, D, armPotentiometer, new ArmOutput(topMotor, bottomMotor));
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

    public class ArmOutput implements PIDOutput {
        private SpeedController topMotor;
        private SpeedController bottomMotor;

        public ArmOutput(SpeedController topMotor, SpeedController bottomMotor) {
            this.topMotor = topMotor;
            this.bottomMotor = bottomMotor;
        }

        public void pidWrite(double output) {
            this.topMotor.set(output);
            this.bottomMotor.set(output);
        }
    }
}
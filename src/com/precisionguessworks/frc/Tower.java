package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.Solenoid;

public class Tower {
    private final Solenoid leftPiston;
    private final Solenoid rightPiston;

    public Tower(Solenoid leftPiston, Solenoid rightPiston) {
        this.leftPiston = leftPiston;
        this.rightPiston = rightPiston;
    }

    public void raise() {
        this.leftPiston.set(true);
        this.rightPiston.set(true);
    }

    public void lower() {
        this.leftPiston.set(false);
        this.rightPiston.set(false);
    }
}
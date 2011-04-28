package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Solenoid;

public class Claw {
    private DigitalInput bumpSwitch;
    private Relay topClawRelay;
    private Relay bottomClawRelay;
    private Solenoid jawOpen;
    private Solenoid jawClose;

    private boolean canCloseJaw = true;

    public Claw(DigitalInput bumpSwitch, Relay topClawRelay, Relay bottomClawRelay, 
            Solenoid jawOpen, Solenoid jawClose) {
        this.bumpSwitch = bumpSwitch;
        this.topClawRelay = topClawRelay;
        this.bottomClawRelay = bottomClawRelay;
        this.jawOpen = jawOpen;
        this.jawClose = jawClose;
    }

    public void setCanCloseJaw(boolean b) {
        this.canCloseJaw = b;
    }

    public boolean getCanCloseJaw() {
        return this.canCloseJaw;
    }

    public boolean isHoldingTube() {
        return !this.bumpSwitch.get();
    }

    public void stop() {
        this.topClawRelay.set(Relay.Value.kOff);
        this.bottomClawRelay.set(Relay.Value.kOff);
    }

    public void turnUp() {
        this.topClawRelay.set(Relay.Value.kReverse);
        this.bottomClawRelay.set(Relay.Value.kForward);
    }

    public void turnDown() {
        this.topClawRelay.set(Relay.Value.kForward);
        this.bottomClawRelay.set(Relay.Value.kReverse);
    }

    public void pushOut() {
        this.topClawRelay.set(Relay.Value.kForward);
        this.bottomClawRelay.set(Relay.Value.kForward);
    }

    public void pullIn() {
        this.topClawRelay.set(Relay.Value.kReverse);
        this.bottomClawRelay.set(Relay.Value.kReverse);
    }

    public void closeJaw() {
        if(this.canCloseJaw) {
            this.jawClose.set(true);
            this.jawOpen.set(false);
        }
    }

    public void openJaw() {
        this.jawClose.set(false);
        this.jawOpen.set(true);
    }

    public static final Relay.Value CLOSE_JAW = Relay.Value.kForward;
    public static final Relay.Value OPEN_JAW = Relay.Value.kReverse;
}
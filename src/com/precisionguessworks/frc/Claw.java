package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Relay;

public class Claw {
    private DigitalInput bumpSwitch;
    private Relay topClawRelay;
    private Relay bottomClawRelay;

    public Claw(DigitalInput bumpSwitch, Relay topClawRelay, Relay bottomClawRelay) {
        this.bumpSwitch = bumpSwitch;
        this.topClawRelay = topClawRelay;
        this.bottomClawRelay = bottomClawRelay;
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
}
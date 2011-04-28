package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.DriverStation;

public class Gamepad {
    public static final int kLeftXAxis = 1;
    public static final int kLeftYAxis = 2;
    public static final int kRightXAxis = 3;
    public static final int kRightYAxis = 4;
    public static final int kDPadXAxis = 5;
    public static final int kDPadYAxis = 6;
    public static final int kLeftAnalogStickButton = 11;
    public static final int kRightAnalogStickButton = 12;

    public class DPadDirection {
        public static final int kCenter = 0;
        public static final int kUp = 1;
        public static final int kUpLeft = 2;
        public static final int kLeft = 3;
        public static final int kDownLeft = 4;
        public static final int kDown = 5;
        public static final int kDownRight = 6;
        public static final int kRight = 7;
        public static final int kUpRight = 8;
    }

    private final DriverStation ds = DriverStation.getInstance();
    private final int port;

    public Gamepad(final int port) {
        this.port = port;
    }

    public double getLeftX() {
        return this.getRawAxis(kLeftXAxis);
    }

    public double getLeftY() {
        return this.getRawAxis(kLeftYAxis);
    }

    public double getRightX() {
        return this.getRawAxis(kRightXAxis);
    }

    public double getRightY() {
        return this.getRawAxis(kRightYAxis);
    }

    public double getRawAxis(final int axis) {
        return this.ds.getStickAxis(this.port, axis);
    }

    public boolean getNumberedButton(final int button) {
        return ((0x1 << (button - 1)) & this.ds.getStickButtons(this.port)) != 0;
    }

    public boolean getLeftPush() {
        return this.getNumberedButton(kLeftAnalogStickButton);
    }

    public boolean getRightPush() {
        return this.getNumberedButton(kRightAnalogStickButton);
    }

    public int getDPad() {
        double x = this.getRawAxis(kDPadXAxis);
        double y = this.getRawAxis(kDPadYAxis);

        if (x < -0.5 && y < -0.5)
            return DPadDirection.kUpLeft;
        if (x < -0.5 && y > 0.5)
            return DPadDirection.kDownLeft;
        if (x > 0.5 && y > 0.5)
            return DPadDirection.kDownRight;
        if (x > 0.5 && y < -0.5)
            return DPadDirection.kUpRight;
        if (y < -0.5)
            return DPadDirection.kUp;
        if (x < -0.5)
            return DPadDirection.kLeft;
        if (y > 0.5)
            return DPadDirection.kDown;
        if (x > 0.5)
            return DPadDirection.kRight;

        return DPadDirection.kCenter;
    }
}
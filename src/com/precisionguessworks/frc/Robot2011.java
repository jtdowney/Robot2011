package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Robot2011 extends IterativeRobot {
    private DigitalInput line1;
    private DigitalInput line2;
    //private RobotDrive drive;
    private Joystick joystick1;
    private Joystick joystick2;
    private CANJaguar frontLeftJaguar;
    private CANJaguar rearLeftJaguar;
    private CANJaguar frontRightJaguar;
    private CANJaguar rearRightJaguar;
    private int lastSeen;
    private boolean left, right;

    private Ultrasonic ultra;
    int sample = 0;
    int sampleCount = 0;
    private boolean motorsOn;
    private boolean changingState;

    public void robotInit() {
        System.out.println("Beginning robot initialization");

        while (true) {
            try {
                this.frontLeftJaguar = new CANJaguar(2, CANJaguar.ControlMode.kVoltage);
                this.frontLeftJaguar.enableControl();

                this.rearLeftJaguar = new CANJaguar(3, CANJaguar.ControlMode.kSpeed);
                this.rearLeftJaguar.setSpeedReference(CANJaguar.SpeedReference.kQuadEncoder);
                this.rearLeftJaguar.setPID(1.5, 0, 0);
                this.rearLeftJaguar.configEncoderCodesPerRev(2);
                this.rearLeftJaguar.enableControl();
                
//                this.frontRightJaguar = new CANJaguar(4);
//                this.rearRightJaguar = new CANJaguar(5);

                break;
            } catch (CANTimeoutException ex) {
                System.out.println("CAN Timeout");
            }
        }

        System.out.println("CAN Initialized");

        this.line1 = new DigitalInput(1);
        this.line2 = new DigitalInput(2);
//        this.drive = new RobotDrive(
//                this.frontLeftJaguar,
//                this.rearLeftJaguar,
//                this.frontRightJaguar,
//                this.rearRightJaguar);
        this.joystick1 = new Joystick(1);
        this.joystick2 = new Joystick(2);

        this.ultra = new Ultrasonic(3, 4);
        this.ultra.setAutomaticMode(true);

        System.out.println("Robot initialized");
    }

    private int count;
    private void driveRobot(double left, double right) throws CANTimeoutException
    {
        rearLeftJaguar.setX(left * 32767, (byte)1);
        frontLeftJaguar.setX(rearLeftJaguar.getOutputVoltage(), (byte)1);
        //frontLeftJaguar.updateSyncGroup((byte)1);
        CANJaguar.updateSyncGroup((byte)1);

        if ((count % 10) == 0)
        {
            System.out.println("output: " + rearLeftJaguar.getOutputVoltage());
            System.out.println("bus:    " + rearLeftJaguar.getBusVoltage());
            System.out.println("% vbus: " + rearLeftJaguar.getOutputVoltage() / rearLeftJaguar.getBusVoltage());
        }

        this.count++;

//        System.out.println("Setting left to " + left + ", " + rearLeftJaguar.getOutputCurrent());
//        if(sample % 2 == 0)
//            System.out.println("Setting left to " + rearLeftJaguar.getOutputVoltage() + ", " + frontLeftJaguar.getOutputVoltage());
    }

    public void disabledPeriodic() {
    }

    public static final double AUTO_SPEED = -.45;
    public static final double TURN_SPEED = -.35;

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int BOTH = 3;

    public static final int NUM_SAMPLES = 3;

    public void autonomousPeriodic() {
        sample += ultra.getRangeMM();
        sampleCount++;

        if(sampleCount == NUM_SAMPLES - 1)
        {
            sample = sample / (sampleCount + 1);
            changingState = motorsOn;
            motorsOn = sample > 500;
            if (motorsOn != changingState)
                System.out.println("Changing state to Motors " +
                        (changingState ? "On" : "Off"));
        }
        if(sampleCount == NUM_SAMPLES)
        {
            sampleCount = 0;
            sample = 0;
        }
        if(!motorsOn && sampleCount == NUM_SAMPLES - 1){
            try {
                driveRobot(0, 0);
                System.out.println("Distance: " + sample + " Stopping the motors!!");
            } catch (CANTimeoutException ex) {
                System.out.println("CAN timeout");
            }
        }
        if(motorsOn){
            try {
                lineTracking();
            } catch (CANTimeoutException ex) {
                System.out.println("CAN timeout");
            }
        }
    }

    private void lineTracking() throws CANTimeoutException{
        left = !line1.get();
        right = !line2.get();

        if(left && right)
        {
            driveRobot(AUTO_SPEED, AUTO_SPEED);
            lastSeen = BOTH;
        }
        else if(left)
        {
            driveRobot(AUTO_SPEED, AUTO_SPEED);
            lastSeen = LEFT;
        }
        else if(right)
        {
            driveRobot(AUTO_SPEED, AUTO_SPEED);
            lastSeen = RIGHT;
        }
        else
        {
            if(lastSeen == LEFT)
                driveRobot(TURN_SPEED, -TURN_SPEED);
            else if(lastSeen == RIGHT)
                driveRobot(-TURN_SPEED, TURN_SPEED);
            else
                driveRobot(0, 0);
        }
    }

    public void teleopPeriodic() {
        try {
            sample++;
            if (sample == 10) {
                System.out.println("$" + ultra.getRangeMM() + "^");
                sample = 0;
            }
            left = !line1.get();
            right = !line2.get();
            if (!(joystick1.getTrigger() || joystick2.getTrigger())) {
                driveRobot(joystick1.getY() / 2, joystick2.getY() / 2);
            } else {
                driveRobot(joystick1.getY(), joystick2.getY());
                //printLineState(left, right);
            }
            //printLineState(left, right);
        } catch (CANTimeoutException ex) {
            System.out.println("CAN timeout");
        }
    }

    private void printLineState(boolean left, boolean right)
    {
        if(left && right)
        {
            System.out.println("Both on");
        }
        else if(left)
        {
            System.out.println("1 on");
        }
        else if(right)
        {
            System.out.println("2 on");
        }
        else
        {
            System.out.print("None on: ");
            if(lastSeen == LEFT)
            {
                System.out.println("left seen last");
            }
            else if(lastSeen == RIGHT)
            {
                System.out.println("right seen last");
            }

        }
    }
}
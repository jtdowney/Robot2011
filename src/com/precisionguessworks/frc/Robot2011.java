package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Robot2011 extends IterativeRobot {
    private DigitalInput line1;
    private DigitalInput line2;
    private RobotDrive drive;
    private Joystick joystick1;
    private Joystick joystick2;
    private CANJaguar frontLeftJaguar;
    private CANJaguar rearLeftJaguar;
    private CANJaguar frontRightJaguar;
    private CANJaguar rearRightJaguar;
    private Encoder leftEncoder;
    private Encoder rightEncoder;
    private int lastSeen;
    private boolean left, right;

    private Ultrasonic ultra;
    int sample = 0;
    int sampleCount = 0;

    public void robotInit() {
        System.out.println("Beginning robot initialization");

        while (true) {
            try {
                this.frontLeftJaguar = new CANJaguar(2);
                this.rearLeftJaguar = new CANJaguar(3);
                this.frontRightJaguar = new CANJaguar(4);
                this.rearRightJaguar = new CANJaguar(5);

                break;
            } catch (CANTimeoutException ex) {
                System.out.println("CAN Timeout");
            }
        }

        this.leftEncoder = new Encoder(5, 6);
        this.leftEncoder.start();
        this.rightEncoder = new Encoder(7, 8);
        this.rightEncoder.start();

        System.out.println("CAN Initialized");

        this.line1 = new DigitalInput(1);
        this.line2 = new DigitalInput(2);
        this.drive = new RobotDrive(
                this.frontLeftJaguar,
                this.rearLeftJaguar,
                this.frontRightJaguar,
                this.rearRightJaguar);
        this.joystick1 = new Joystick(1);
        this.joystick2 = new Joystick(2);

        this.ultra = new Ultrasonic(3, 4);
        this.ultra.setAutomaticMode(true);

        System.out.println("Robot initialized");
    }

    public void disabledPeriodic() {
    }

    public static final double AUTO_SPEED = -.3;
    public static final double TURN_SPEED = -.25;

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int BOTH = 3;



    public void autonomousPeriodic() {
        if(sampleCount == 4)
        {
            sample >>= 4;
        }
        if(sampleCount == 5)
        {
            sampleCount = 0;
            sample = 0;
        }
        
        sample += ultra.getRangeMM();
        sampleCount++;

        left = !line1.get();
        right = !line2.get();

        if(left && right)
        {
            drive.setLeftRightMotorOutputs(AUTO_SPEED, AUTO_SPEED);
            lastSeen = BOTH;
            System.out.println("Both on");
        }
        else if(left)
        {
            drive.setLeftRightMotorOutputs(AUTO_SPEED, AUTO_SPEED);
            lastSeen = LEFT;
            System.out.println("1 on");
        }
        else if(right)
        {
            drive.setLeftRightMotorOutputs(AUTO_SPEED, AUTO_SPEED);
            lastSeen = RIGHT;
            System.out.println("2 on");
        }
        else
        {
            if(lastSeen == LEFT)
                drive.setLeftRightMotorOutputs(TURN_SPEED, -TURN_SPEED);
            else if(lastSeen == RIGHT)
                drive.setLeftRightMotorOutputs(-TURN_SPEED, TURN_SPEED);
            else
                drive.setLeftRightMotorOutputs(0, 0);
            System.out.println("None on");
        }
    }

    public void teleopPeriodic() {
        sample++;
        if(sample == 10)
        {
            System.out.println("$" + ultra.getRangeMM() + "@$");
            System.out.println("left: " + leftEncoder.get() +
                    " right: " + rightEncoder.get());
            sample = 0;
        }

        left = !line1.get();
        right = !line2.get();

        if(!(joystick1.getTrigger() || joystick2.getTrigger()))
            this.drive.setLeftRightMotorOutputs(joystick1.getY() / 2, joystick2.getY() / 2);
        else
            this.drive.setLeftRightMotorOutputs(joystick1.getY(), joystick2.getY());
        
//        if(left && right)
//        {
//            System.out.println("Both on");
//        }
//        else if(left)
//        {
//            System.out.println("1 on");
//        }
//        else if(right)
//        {
//            System.out.println("2 on");
//        }
//        else
//        {
//            System.out.print("None on: ");
//            if(lastSeen == LEFT)
//            {
//                System.out.println("left seen last");
//            }
//            else if(lastSeen == RIGHT)
//            {
//                System.out.println("right seen last");
//            }
//
//        }
    }
}
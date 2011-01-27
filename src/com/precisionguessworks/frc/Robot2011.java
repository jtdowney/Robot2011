package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.camera.AxisCamera;
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
    private Relay speedRelay;
    private int lastSeen;
    private boolean left, right;

    private Gyro gyro;
    private ADXL345_I2C accel;
    private AxisCamera camera;

    private Ultrasonic ultra;
    int sample = 0;
    int sampleCount = 0;
    int missCount = 0;
    private boolean motorsOn = true;
    private boolean changingState;

    public void robotInit() {
        System.out.println("Beginning robot initialization");

        while (true) {
            try {
                this.frontLeftJaguar = new CANJaguar(2, CANJaguar.ControlMode.kPercentVbus);
                this.frontLeftJaguar.enableControl();

                this.rearLeftJaguar = new CANJaguar(3, CANJaguar.ControlMode.kPercentVbus);
//                this.rearLeftJaguar.setSpeedReference(CANJaguar.SpeedReference.kQuadEncoder);
//                this.rearLeftJaguar.setPID(1.5, 0, 0);
//                this.rearLeftJaguar.configEncoderCodesPerRev(2);
//                this.rearLeftJaguar.enableControl();
                
                this.frontRightJaguar = new CANJaguar(4, CANJaguar.ControlMode.kPercentVbus);
                this.rearRightJaguar = new CANJaguar(5, CANJaguar.ControlMode.kPercentVbus);

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

        this.gyro = new Gyro(2);
        this.accel = new ADXL345_I2C(4, ADXL345_I2C.DataFormat_Range.k8G);

        this.speedRelay = new Relay(2);

//        camera = AxisCamera.getInstance();

        this.ultra = new Ultrasonic(3, 4);
        this.ultra.setAutomaticMode(true);

        System.out.println("Robot initialized");
    }

    private int count;
    private void driveRobot(double left, double right) throws CANTimeoutException
    {
        rearLeftJaguar.setX(left, (byte)1);
        frontLeftJaguar.setX(left, (byte)1);
        rearRightJaguar.setX(-right, (byte)1);
        frontRightJaguar.setX(-right, (byte)1);
        CANJaguar.updateSyncGroup((byte)1);

        if ((count % 10) == 0)
        {
            System.out.println("accel: " + accel.getAcceleration(ADXL345_I2C.Axes.kX));
//            System.out.println("gyro: " + gyro.getAngle());
//            System.out.println("output: " + rearLeftJaguar.getOutputVoltage());
//            System.out.println("bus:    " + rearLeftJaguar.getBusVoltage());
//            System.out.println("% vbus: " + rearLeftJaguar.getOutputVoltage() / rearLeftJaguar.getBusVoltage());
        }

        this.count++;

//        System.out.println("Setting left to " + left + ", " + rearLeftJaguar.getOutputCurrent());
//        if(sample % 2 == 0)
//            System.out.println("Setting left to " + rearLeftJaguar.getOutputVoltage() + ", " + frontLeftJaguar.getOutputVoltage());
    }

    public void disabledInit() {
        System.out.println("Resetting gyro.");
        gyro.reset();
    }

    public static final double AUTO_SPEED = -.4;
    public static final double TURN_SPEED = -.28;

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int BOTH = 3;

    public static final int NUM_SAMPLES = 3;
    public final int ULTRA_SONIC_THRESHOLD = 6000;
    int tmp;

    public void autonomousPeriodic() {
//        tmp = ((int)ultra.getRangeMM());
        tmp = 8000; //testing
        if(tmp < ULTRA_SONIC_THRESHOLD){
            sample += tmp;
            sampleCount++;
        }
        else{
            missCount++;
        }

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
            System.out.println("Miss Count: " + missCount);
            missCount = 0;
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

    public double targetSpeedRight = 0;
    public double actualSpeedRight = 0;
    public double rightSpeedChange = 0.07;
    public double targetSpeedLeft = 0;
    public double actualSpeedLeft = 0;
    public double leftSpeedChange = 0.07;

    private void lineTracking() throws CANTimeoutException{
        left = line1.get();
        right = line2.get();

        if(left && right)
        {
            targetSpeedLeft = AUTO_SPEED;
            targetSpeedRight = AUTO_SPEED;
            lastSeen = BOTH;
        }
        else if(left)
        {
            //driveRobot(targetSpeedRight, targetSpeedLeft);
            targetSpeedLeft = AUTO_SPEED;
            targetSpeedRight = AUTO_SPEED;
            lastSeen = LEFT;
        }
        else if(right)
        {
            //driveRobot(targetSpeedRight, targetSpeedLeft);
            targetSpeedRight = AUTO_SPEED;
            targetSpeedLeft = AUTO_SPEED;
            lastSeen = RIGHT;
        }
        else
        {
            if(lastSeen == LEFT){
                targetSpeedLeft = -TURN_SPEED;
                targetSpeedRight = TURN_SPEED;
            }
            else if(lastSeen == RIGHT){
                targetSpeedRight = -TURN_SPEED;
                targetSpeedLeft = TURN_SPEED;
            }
            else{
                driveRobot(0, 0);
            }
        }
        
        System.out.print("l: " + targetSpeedLeft + ", " + actualSpeedLeft + ", ");
        if(targetSpeedLeft > (actualSpeedLeft + leftSpeedChange))
            targetSpeedLeft = actualSpeedLeft + leftSpeedChange;
        else if(targetSpeedLeft < (actualSpeedLeft - leftSpeedChange))
            targetSpeedLeft = actualSpeedLeft - leftSpeedChange;
        System.out.println(targetSpeedLeft);

        actualSpeedLeft = targetSpeedLeft;

        System.out.print("r: " + targetSpeedRight + ", " + actualSpeedRight + ", ");
        if(targetSpeedRight > actualSpeedRight + rightSpeedChange)
            targetSpeedRight = actualSpeedRight + rightSpeedChange;
        else if(targetSpeedRight < actualSpeedRight - rightSpeedChange)
            targetSpeedRight = actualSpeedRight - rightSpeedChange;
        System.out.println(targetSpeedRight);

        actualSpeedRight = targetSpeedRight;

        driveRobot(targetSpeedLeft, targetSpeedRight);
    }

    public void teleopPeriodic() {
        try {
            sample++;
            if (sample == NUM_SAMPLES) {
                //System.out.println("$" + ultra.getRangeMM() + "^");
                sample = 0;
                missCount = 0;
            }
            if(sample > ULTRA_SONIC_THRESHOLD)
                missCount++;
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

        speedRelay.set(Relay.Value.kReverse);
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
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
    private AnalogChannel pot; //port 5
    private DigitalInput actuator; //port 7
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

    PIDController gyroPID;
    GyroPIDOutput gyroPIDOutput;

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

        this.pot = new AnalogChannel(5);
        this.actuator = new DigitalInput(7);

        this.gyro = new Gyro(2);

        gyroPIDOutput = new GyroPIDOutput();
        gyroPID = new PIDController(.015, 0.01, .06, gyro, gyroPIDOutput);
        gyroPID.setContinuous();
        gyroPID.setOutputRange(-.32, .32);
        gyroPID.setTolerance(5);

        

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
        gyroPID.disable();
    }

    public static final double AUTO_SPEED = -.35;
    public static final double TURN_SPEED = -.28;

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int BOTH = 3;

    public static final int NUM_SAMPLES = 3;
    public final int ULTRA_SONIC_THRESHOLD = 6000;
    int tmp;

    public void autonomousInit()
    {
        gyroPID.enable();
    }

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
//            System.out.println("Miss Count: " + missCount);
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


    private int relAngle = 0;   // The relative angle we want to travel (since we last saw the line)
//    private double offsAngle = 0;  // The angle the gyro was at when we last saw the line

    private void lineTracking() throws CANTimeoutException{
        left = !line1.get();
        right = !line2.get();

        if(left && right)
        {
            lastSeen = BOTH;
            relAngle = 0;
            gyro.reset();
        }
        else if(left)
        {
            lastSeen = LEFT;
            relAngle = 0;
            gyro.reset();
        }
        else if(right)
        {
            lastSeen = RIGHT;
            relAngle = 0;
            gyro.reset();
        }
        else
        {
            if(lastSeen == LEFT){
                if(gyroPID.onTarget() && relAngle == -45)
                    relAngle = 45;
                else
                    relAngle = -45;
            }
            else if(lastSeen == RIGHT){
                if(gyroPID.onTarget() && relAngle == 45)
                    relAngle = -45;
                else
                    relAngle = 45;
            }
            else{
                if(gyroPID.onTarget() && relAngle == 45)
                    relAngle = -45;
                else
                    relAngle = 45;
            }
        }

       gyroPID.setSetpoint(relAngle);
       if(relAngle == 0)
           driveRobot(AUTO_SPEED + gyroPIDOutput.getValue(), AUTO_SPEED - gyroPIDOutput.getValue());
       else
           driveRobot(gyroPIDOutput.getValue(), -gyroPIDOutput.getValue());

       System.out.println("output: (" + gyroPIDOutput.getValue() + ")");

        System.out.println("pid:  " + gyroPID.getSetpoint() + ", " + gyroPID.getError());
        System.out.println("gyro: " + gyro.getAngle() + "\n");
        //driveRobot(targetSpeedLeft, targetSpeedRight);
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

        System.out.println("accel: " + accel.getAcceleration(ADXL345_I2C.Axes.kX));
//        System.out.println("Pot: " + pot.getAverageValue() + " Switch: " + this.actuator.get());

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
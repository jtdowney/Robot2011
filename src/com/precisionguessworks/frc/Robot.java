package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.can.CANTimeoutException;
import java.io.PrintStream;
import java.util.Calendar;
import javax.microedition.io.Connector;
import com.sun.squawk.microedition.io.FileConnection;
import com.sun.squawk.util.Arrays;

public class Robot extends IterativeRobot {
    private AnalogChannel pot;
    private PrintStream file;
    // inputs
    private LogitechDualActionGamepad leftGamepad;
    private LogitechDualActionGamepad rightGamepad;

    // subsystems
    private Claw claw;
    private CANJaguar topArmMotor;
    private Tower tower;
    private Arm arm;
    private Drive drive;
    private Compressor compressor;

    private long counter;
    private int setpoint;

    public void robotInit() {
        System.out.println("Initializing robot");

        this.leftGamepad = new LogitechDualActionGamepad(1);
        this.rightGamepad = new LogitechDualActionGamepad(2);

        CANJaguar frontLeftMotor;
        CANJaguar rearLeftMotor;
        CANJaguar frontRightMotor;
        CANJaguar rearRightMotor;
        //CANJaguar topArmMotor;
        CANJaguar bottomArmMotor;

        System.out.println("Initializing CAN bus");
        while (true) {
            try {
                frontLeftMotor = new CANJaguar(1);
                rearLeftMotor = new CANJaguar(2);
                frontRightMotor = new CANJaguar(3);
                rearRightMotor = new CANJaguar(4);
                topArmMotor = new CANJaguar(5);
                bottomArmMotor = new CANJaguar(6);

                break;
            } catch (CANTimeoutException exception) {
                System.out.println("CAN Timeout");
            }
        }
        System.out.println("CAN bus initialized");

        this.claw = new Claw(new DigitalInput(6), new Relay(1), new Relay(2));
        pot = new AnalogChannel(3);
        this.arm = new Arm(pot, topArmMotor, bottomArmMotor, this.tower);
        this.tower = new Tower(new Solenoid(1), new Solenoid(2));
        this.drive = new Drive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor, new Solenoid(3));

        this.compressor = new Compressor(5, 3);
        this.compressor.start();

        // turn on photoswitches
        new Solenoid(8).set(true);

        System.out.println("Robot initialized");
    }

    // Disabled mode
    public void disabledInit() {
    }

    public void disabledContinuous() {
    }

    public void disabledPeriodic() {
        this.updateDashboard();
    }

    // Autonomous mode
    public static String paddingString(String s, int n, char c, boolean paddingLeft) {
        if (s == null) {
            return s;
        }

        int add = n - s.length(); // may overflow int size... should not be a problem in real life
        if(add <= 0) {
            return s;
        }

        StringBuffer str = new StringBuffer(s);
        char[] ch = new char[add];
        Arrays.fill(ch, c);

        if(paddingLeft){
            str.insert(0, ch);
        } else {
            str.append(ch);
        }
        
        return str.toString();
    }

    public void autonomousInit() {
        try {
            Calendar cal = Calendar.getInstance();

            String year = new Integer(cal.get(Calendar.YEAR)).toString();
            String month = new Integer(cal.get(Calendar.MONTH) + 1).toString();
            String date = new Integer(cal.get(Calendar.DATE)).toString();
            String hour = new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString();
            String minute = new Integer(cal.get(Calendar.MINUTE)).toString();
            String second = new Integer(cal.get(Calendar.SECOND)).toString();
            String name =
                    year +
                    Robot.paddingString(month, 2, '0', true) +
                    Robot.paddingString(date, 2, '0', true) + "-" +
                    Robot.paddingString(hour, 2, '0', true) +
                    Robot.paddingString(minute, 2, '0', true) +
                    Robot.paddingString(second, 2, '0', true);

            name += "_" + Arm.kP + "_" + Arm.kI + "_" + Arm.kD + ".csv";

            FileConnection connection = (FileConnection) Connector.open("file:///" + name, Connector.WRITE);
            connection.create();
            
            this.file = new PrintStream(connection.openDataOutputStream());
        } catch (Exception e) {}

        this.file.println("time,setpoint,value,drive");
        this.counter = 0;
        this.setpoint = 300;

        this.arm.setPosition(this.setpoint);
    }

    public void autonomousContinuous() {
    }

    public void autonomousPeriodic() {
        this.updateDashboard();

        try {
            final String output = (this.counter++) + "," + this.setpoint + "," + this.pot.getValue() + "," + this.topArmMotor.getX();

            this.file.println(output);
            System.out.println(output);
        } catch (CANTimeoutException ex) {}
    }

    // Teleop mode
    public void teleopInit() {
    }

    public void teleopContinuous() {
    }

    public void teleopPeriodic() {
        this.updateDashboard();

        this.controlDrive();
        this.controlArm();
        this.controlClaw();
    }

    private void controlDrive() {
        this.drive.tankDrive(this.leftGamepad.getLeftY(), this.leftGamepad.getRightY());
    }

    private void controlArm() {
        if (this.rightGamepad.getNumberedButton(7)) {
            this.arm.manualDrive(this.rightGamepad.getLeftY() * 0.5);
        }
    }
    
    private void controlClaw() {
        if (this.rightGamepad.getNumberedButton(3)) {
            this.claw.pushOut();
        } else if (this.rightGamepad.getNumberedButton(1)) {
            this.claw.turnUp();
        } else if (this.rightGamepad.getNumberedButton(2)) {
            this.claw.turnDown();
        } else if (this.claw.isHoldingTube()) {
            this.claw.stop();
        } else {
            this.claw.pullIn();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Dashboard Update">
    void updateDashboard() {
        Dashboard lowDashData = DriverStation.getInstance().getDashboardPackerLow();
        lowDashData.addCluster();
        {
            lowDashData.addCluster();
            {     //analog modules
                lowDashData.addCluster();
                {
                    for (int i = 1; i <= 8; i++) {
                        lowDashData.addFloat((float) AnalogModule.getInstance(1).getAverageVoltage(i));
                    }
                }
                lowDashData.finalizeCluster();
                lowDashData.addCluster();
                {
                    for (int i = 1; i <= 8; i++) {
                        lowDashData.addFloat((float) AnalogModule.getInstance(2).getAverageVoltage(i));
                    }
                }
                lowDashData.finalizeCluster();
            }
            lowDashData.finalizeCluster();

            lowDashData.addCluster();
            { //digital modules
                lowDashData.addCluster();
                {
                    lowDashData.addCluster();
                    {
                        int module = 4;
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayForward());
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayForward());
                        lowDashData.addShort(DigitalModule.getInstance(module).getAllDIO());
                        lowDashData.addShort(DigitalModule.getInstance(module).getDIODirection());
                        lowDashData.addCluster();
                        {
                            for (int i = 1; i <= 10; i++) {
                                lowDashData.addByte((byte) DigitalModule.getInstance(module).getPWM(i));
                            }
                        }
                        lowDashData.finalizeCluster();
                    }
                    lowDashData.finalizeCluster();
                }
                lowDashData.finalizeCluster();

                lowDashData.addCluster();
                {
                    lowDashData.addCluster();
                    {
                        int module = 6;
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayForward());
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayReverse());
                        lowDashData.addShort(DigitalModule.getInstance(module).getAllDIO());
                        lowDashData.addShort(DigitalModule.getInstance(module).getDIODirection());
                        lowDashData.addCluster();
                        {
                            for (int i = 1; i <= 10; i++) {
                                lowDashData.addByte((byte) DigitalModule.getInstance(module).getPWM(i));
                            }
                        }
                        lowDashData.finalizeCluster();
                    }
                    lowDashData.finalizeCluster();
                }
                lowDashData.finalizeCluster();

            }
            lowDashData.finalizeCluster();

            lowDashData.addByte(Solenoid.getAllFromDefaultModule());
        }
        lowDashData.finalizeCluster();
        lowDashData.commit();
    }// </editor-fold>
}
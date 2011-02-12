package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Robot extends IterativeRobot {
    // inputs
    private LogitechDualActionGamepad leftGamepad;
    private LogitechDualActionGamepad rightGamepad;

    // subsystems
    private Claw claw;
    private Tower tower;
    private Arm arm;
    private Drive drive;

    public void robotInit() {
        System.out.println("Initializing robot");

        this.leftGamepad = new LogitechDualActionGamepad(1);
        this.rightGamepad = new LogitechDualActionGamepad(2);

        CANJaguar frontLeftMotor;
        CANJaguar rearLeftMotor;
        CANJaguar frontRightMotor;
        CANJaguar rearRightMotor;
//        CANJaguar topArmMotor;
//        CANJaguar bottomArmMotor;

        System.out.println("Initializing CAN bus");
        while (true) {
            try {
                frontLeftMotor = new CANJaguar(1);
                rearLeftMotor = new CANJaguar(2);
                frontRightMotor = new CANJaguar(3);
                rearRightMotor = new CANJaguar(4);
//                topArmMotor = new CANJaguar(6);
//                bottomArmMotor = new CANJaguar(7);

                break;
            } catch (CANTimeoutException exception) {
                System.out.println("CAN Timeout");
            }
        }
        System.out.println("CAN bus initialized");

        this.claw = new Claw(new DigitalInput(6), new Relay(1), new Relay(2));
//        this.arm = new Arm(new AnalogChannel(3), topArmMotor, bottomArmMotor, this.tower);
        this.tower = new Tower(new Solenoid(1), new Solenoid(2));
        this.drive = new Drive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor, new Solenoid(3));

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
    public void autonomousInit() {
    }

    public void autonomousContinuous() {
    }

    public void autonomousPeriodic() {
        this.updateDashboard();
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
            this.arm.manualDrive(this.rightGamepad.getLeftY());
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
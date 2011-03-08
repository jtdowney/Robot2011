package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Robot extends IterativeRobot {
    private static final int kOtherPickupPosition = 0;
    private static final int kMediumPickupPosition = 1;
    private static final int kLowPickupPosition = 2;

    // state
    private long counter;
    private DataFile armDataFile;
    private boolean shiftButtonPressed = false;
    private boolean pickingUp = false;
    private Timer pickupTimer = new Timer();
    private int pickupPosition = kOtherPickupPosition;

    // inputs
    private Gamepad leftGamepad;
    private Gamepad rightGamepad;

    // subsystems
    private Claw claw;
    private Tower tower;
    private Arm arm;
    private Drive drive;
    private Compressor compressor;

    public void robotInit() {
        System.out.println("Initializing robot");
        
        try {
            this.armDataFile = new DataFile("arm_data");
            this.armDataFile.writeln("time,setpoint,value,drive");
        } catch (Exception exception) {
            System.out.println("Unable to open data file for writing");
        }

        this.leftGamepad = new Gamepad(1);
        this.rightGamepad = new Gamepad(2);

        System.out.println("Initializing subsystems");

        CANJaguar frontLeftMotor;
        CANJaguar rearLeftMotor;
        CANJaguar frontRightMotor;
        CANJaguar rearRightMotor;
        CANJaguar topArmMotor;
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
        this.arm = new Arm(new AnalogChannel(3), topArmMotor, bottomArmMotor);
        this.tower = new Tower(new Solenoid(1), new Solenoid(2));
        this.drive = new Drive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor, new Solenoid(3), new Solenoid(4));

        this.compressor = new Compressor(5, 3);
        this.compressor.start();

        System.out.println("Subsystems initialized");
        System.out.println("Robot initialized");
    }

    public void reset() {
        this.counter = 0;
        this.shiftButtonPressed = false;
        this.pickingUp = false;
        this.pickupPosition = kOtherPickupPosition;

        this.drive.shiftDown();
        this.tower.raise();
        this.arm.resetPIDController();
        this.arm.resetSpeedLimiter();

        // turn on photoswitches
        //new Solenoid(8).set(true);
    }

    // Disabled mode
    public void disabledInit() {
        this.reset();
    }

    public void disabledContinuous() {
    }

    public void disabledPeriodic() {
        this.updateDashboard();
    }

    // Autonomous mode
    public void autonomousInit() {
        this.reset();
    }

    public void autonomousContinuous() {
    }

    public void autonomousPeriodic() {
        this.updateDashboard();
        this.writeArmData();
    }

    // Teleop mode
    public void teleopInit() {
        this.reset();
    }

    public void teleopContinuous() {
    }

    public void teleopPeriodic() {
        this.updateDashboard();
        this.writeArmData();

        this.controlDrive();
        this.controlArm();
        this.controlClaw();
//        this.controlTower();
    }

//    private void controlTower() {
//        if (this.rightGamepad.getNumberedButton(9) && this.pot.getValue() < 310)
//        {
//            if(this.towerButtonPressed)
//            {
//                //Don't change the state if the button is being held down
//            }
//            else if (this.towerPosition == kUP)
//            {
//                this.tower.lower();
//                System.out.println("lowering tower");
//                this.towerPosition = kDOWN;
//                this.towerButtonPressed = true;
//            }
//            else
//            {
//                this.tower.raise();
//                System.out.println("raising tower");
//                this.towerPosition = kUP;
//                this.towerButtonPressed = true;
//            }
//        }
//        else
//        {
//            this.tower.hold();
//            this.towerButtonPressed = false;
//        }
//    }

    private void controlDrive() {
        if (this.leftGamepad.getNumberedButton(8))
        {
            if (!this.shiftButtonPressed) {
                this.drive.toggleShift();
                this.shiftButtonPressed = true;
            }
        }
        else
        {
            this.shiftButtonPressed = false;
        }
        
        this.drive.tankDrive(this.leftGamepad.getLeftY(), this.leftGamepad.getRightY());
    }

    private void controlArm() {
        if (this.counter % 10 == 0) {
            System.out.println("Current pot value: " + this.arm.getCurrentPosition());
        }

        if (this.rightGamepad.getNumberedButton(1) || this.pickingUp) {
            this.pickup();
        }
        else if(this.rightGamepad.getNumberedButton(2))
        {
            //this.arm.setPosition(200);
            this.arm.setPosition(300);
        }
        else if (this.rightGamepad.getNumberedButton(3))
        {
            this.arm.setPosition(380);
            //this.arm.setPosition(480);
        }
        else if (this.rightGamepad.getNumberedButton(4))
        {
            this.arm.setPosition(515);
        }

        // here be dragons
        //if (this.rightGamepad.getNumberedButton(7)) {
        //    this.arm.manualDrive(this.rightGamepad.getLeftY() * 0.5);
        //}
    }
    
    private void controlClaw() {
        if (this.rightGamepad.getNumberedButton(5)) {
            this.claw.pushOut();
        } else if (this.rightGamepad.getNumberedButton(6)) {
            this.claw.turnUp();
        } else if (this.rightGamepad.getNumberedButton(8)) {
            this.claw.turnDown();
        } else if (this.claw.isHoldingTube()) {
            this.claw.stop();
        } else {
            this.claw.pullIn();
        }
    }

    private void pickup() {
        if (!this.pickingUp) {
            // start picking up
            this.pickingUp = true;

            this.pickupPosition = kOtherPickupPosition;
            this.arm.setPosition(200);

            System.out.println("Starting pickup, going to medium position");
        }

        if (this.pickupPosition == kOtherPickupPosition && this.arm.getCurrentPosition() >= 180 && this.arm.getCurrentPosition() <= 275) {
            // we've reached our medium position, drop the tower
            this.pickupPosition = kMediumPickupPosition;

            this.tower.lower();
            this.pickupTimer.start();

            System.out.println("Lowering tower");
        }

        if (this.pickupPosition == kMediumPickupPosition && this.pickupTimer.get() >= 0.2) {
            // we've moved the tower, go to the floor or hold position
            this.pickupPosition = kLowPickupPosition;

            this.pickupTimer.stop();
            this.pickupTimer.reset();

            System.out.println("Going to lower position");

            if (this.claw.isHoldingTube()) {
                this.arm.setPosition(120);
                // we're holding a tube in the lowest position, our work here is done
                this.pickingUp = false;

                System.out.println("I haz a tube");
            } else {
                this.arm.setPosition(175);
                System.out.println("I can haz tube?");
            }
        }

        if (this.pickupPosition == kLowPickupPosition && this.claw.isHoldingTube()) {
            // we've acquired the tube in the floor position, raise the tower
            this.pickupPosition = kMediumPickupPosition;

            this.tower.raise();
            this.pickupTimer.start();

            System.out.println("I have you now, raising towner");
        }
    }

    private void writeArmData() {
        final String output =
                (this.counter++) + "," +
                this.arm.getTargetPosition() + "," +
                this.arm.getCurrentPosition() + "," +
                this.arm.getCurrentSpeed();

        this.armDataFile.writeln(output);
        //System.out.println(output);
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
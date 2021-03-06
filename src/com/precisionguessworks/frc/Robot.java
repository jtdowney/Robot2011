package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Robot extends IterativeRobot {
    private static final int kArmBaseValue = 200;
    
    private static final int kOtherPickupPosition = 0;
    private static final int kMediumPickupPosition = 1;
    private static final int kLowPickupPosition = 2;

    // autonomous tuning constants
    private static final double kAutoStraightSpeed = -.4;
    private static final double kAutoTurnSpeed = -.3;
    private static final double kAutoLowScale = 0;
    private static final double kAutoHighScale = .4;
    
    private int lineLastSeen = kNone;
    private static final int kNone = 0;
    private static final int kLeft = 1;
    private static final int kRight = 2;


    // state
    private long counter;
    private DataFile armDataFile;
    private DataFile autonDataFile;
    private boolean shiftButtonPressed = false;
    private boolean pickingUp = false;
    private Timer pickupTimer = new Timer();
    private int pickupPosition = kOtherPickupPosition;

    // inputs
    private Gamepad driverGamepad;
    private Gamepad operatorGamepad;

    // subsystems
    private Claw claw;
    private Tower tower;
    private Arm arm;
    private Drive drive;
    private Compressor compressor;
    private Minibot minibot;

    private DigitalInput leftLine, middleLine, rightLine;
    private DriverStationLCD lcd = DriverStationLCD.getInstance();
//    private Solenoid linePower;
    private boolean cancellingPickup;
    private boolean pickupButtonReleased;
    


    public void robotInit() {
        System.out.println("Initializing robot");
        
        try {
            this.armDataFile = new DataFile("arm_data");
            this.armDataFile.writeln("time,setpoint,value,drive");
        } catch (Exception exception) {
            System.out.println("Unable to open data file for writing");
        }

        try {
            this.autonDataFile = new DataFile("auton_log");
        } catch (Exception exception) {
            System.out.println("Unable to open auton file for writing");
        }

        this.driverGamepad = new Gamepad(1);
        this.operatorGamepad = new Gamepad(2);

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
                lcd.println(DriverStationLCD.Line.kMain6, 1, "CAN Timeout!!!");
                lcd.updateLCD();
            }
        }
        
        System.out.println("CAN bus initialized");

        this.claw = new Claw(new DigitalInput(6), new Relay(1), new Relay(2), new Solenoid(5), new Solenoid(6));
        this.arm = new Arm(new AnalogChannel(3), topArmMotor, bottomArmMotor);
        this.tower = new Tower(new Solenoid(1), new Solenoid(2));
        this.drive = new Drive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor, new Solenoid(3), new Solenoid(4));
        this.minibot = new Minibot(new Solenoid(7), new Solenoid(8), new Servo(1));

        this.compressor = new Compressor(5, 3);
        this.compressor.start();

//        linePower = new Solenoid(8);
//        linePower.set(true);
        this.leftLine = new DigitalInput(1);
        this.middleLine = new DigitalInput(2);
        this.rightLine = new DigitalInput(3);


        System.out.println("Subsystems initialized");
        System.out.println("Robot initialized");
        lcd.println(DriverStationLCD.Line.kUser2, 1, "Robot intialized");
        lcd.updateLCD();
    }

    public void cancelPickup() {
        this.pickingUp = false;
        this.pickupPosition = kOtherPickupPosition;
        this.tower.raise();
        this.claw.closeJaw();
    }

    public void reset() {
        this.shiftButtonPressed = false;
        this.pickingUp = false;
        this.pickupPosition = kOtherPickupPosition;

        this.drive.shiftUp();
        this.tower.raise();
        this.arm.resetPIDController();
        this.arm.resetSpeedLimiter();
        this.minibot.undeploy();

        this.claw.closeJaw();

        this.lineFollowReset();
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
        this.arm.holdPosition();
    }

    // Autonomous mode
    public void autonomousInit() {
        this.reset();
        this.lineFollowReset();
        System.out.println("Raising arm.");
        writeAutonData("Raising arm.");
        this.arm.setPosition(465);
        atTee = false;
        drive.shiftUp();
    }

    public void autonomousContinuous() {
    }

//    private Timer teaTimer = new Timer();   // After we see all 3 sensors on, wait to see if its a branch or the end
    private boolean atTee = false;          // True when we hit the end
    private Timer backAwayTimer = new Timer();  // Wait to back up after placing tube
    private double time = 0;

    public void autonomousPeriodic() {
        this.updateDashboard();
        this.arm.schedule();
        this.writeArmData();

        if(atTee)
        {
            time = backAwayTimer.get();
            if(time == 0) {
                this.drive.tankDrive(0, 0);
                writeAutonData("Starting back away timer.");
                System.out.println("Starting back away timer");
                this.backAwayTimer.reset();
                this.backAwayTimer.start();
            }
            if(time > 1 && time < 1.1) {
                System.out.println("Releasing tube");
                writeAutonData("Releasing tube");
                this.claw.openJaw();
                this.claw.setCanCloseJaw(false);
                this.claw.turnDown();
            }
            else if(time > 1.9 && time < 2) {
                writeAutonData("Backing up");
                System.out.println("Backing up");
                this.drive.tankDrive(-kAutoStraightSpeed, -kAutoStraightSpeed);
            }
            else if(time > 2 && time < 3.5) {
                // We've stopped - release the tube
                writeAutonData("lowering arm & backing up");
                System.out.println("lowering arm & backing up");
                this.arm.setPosition(450);
                this.drive.tankDrive(-kAutoStraightSpeed, -kAutoStraightSpeed);
            }
            else if(time > 3.5) {
                writeAutonData("Backed up. Stopping.");
                System.out.println("Backed up. Stopping.");
                this.drive.tankDrive(0, 0);
            }
            else {
                this.drive.tankDrive(0, 0);
            }
        }
        else
        {
            this.claw.stop();
            this.arm.setPosition(465);
            this.followLine();
        }
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
        this.controlMinibot();
        this.arm.schedule();

        boolean left = leftLine.get();
        boolean middle = middleLine.get();
        boolean right = rightLine.get();
        System.out.println("line: " + left + " " + middle + " " + right);
    }

    private int currTurnDir = kNone;
    private Timer turnTimer = new Timer();
    private boolean firstTurn = true;      //True when we are doing the first turn to re-find the line

    private void lineFollowReset()
    {
        atTee = false;
        currTurnDir = kNone;
        turnTimer.stop();
        turnTimer.reset();
        backAwayTimer.stop();
        backAwayTimer.reset();
        firstTurn = true;
    }

    private void followLine() {

        // Follow a line
        boolean left = leftLine.get();
        boolean middle = middleLine.get();
        boolean right = rightLine.get();

        writeAutonData("line: " + left + middle + right);

        if(atTee) {
            if(counter % 10 == 0)
                System.out.println("Not moving - atTee true");
            this.drive.tankDrive(0,0);
        }
        else if(left && middle && right) {
            atTee = true;
        }
        else if (left && right) {
            //Fork - go left
            this.drive.tankDrive(-kAutoTurnSpeed, kAutoStraightSpeed);
        }
        else if(left && middle) {
            this.drive.tankDrive(kAutoStraightSpeed * kAutoHighScale,
                    kAutoStraightSpeed);
            System.out.println("Going slightly left");
        }
        else if (middle && right) {
            this.drive.tankDrive(kAutoStraightSpeed, 
                    kAutoStraightSpeed * kAutoHighScale);
            System.out.println("Going slightly right");
        }
        else if (left) {
            this.drive.tankDrive(kAutoTurnSpeed * kAutoLowScale,
                    kAutoTurnSpeed);
            lineLastSeen = kLeft;
            System.out.println("Going left");
        }
        else if (middle) {
            this.drive.tankDrive(kAutoStraightSpeed, kAutoStraightSpeed);
            lineLastSeen = kNone;
        }
        else if (right) {
            this.drive.tankDrive(kAutoTurnSpeed,
                    kAutoTurnSpeed * kAutoLowScale);
            lineLastSeen = kRight;
            System.out.println("Going right");
        }
        else {
            if(currTurnDir == kNone) {
                System.out.print("Starting turn: ");
                turnTimer.reset();
                turnTimer.start();
                if(lineLastSeen == kRight) {
                    currTurnDir = kRight;
                    firstTurn = true;
                    System.out.println("right");
                }
                else {
                    currTurnDir = kLeft;
                    firstTurn = true;
                    System.out.println("left");
                }
            }
            else if((firstTurn && turnTimer.get() >= 2)) {
                firstTurn = false;
                System.out.println("Switching turn directions");
                // Switch directions
                if (currTurnDir == kLeft) {
                    currTurnDir = kRight;
                }
                else {
                    currTurnDir = kLeft;
                }
            }

            if (currTurnDir == kRight) {
                this.drive.tankDrive(kAutoTurnSpeed, -kAutoTurnSpeed * kAutoHighScale);
            }
            else if(currTurnDir == kLeft) {
                this.drive.tankDrive(-kAutoTurnSpeed * kAutoHighScale, kAutoTurnSpeed);
            }
        }
    }

    private void controlDrive() {
        if (this.driverGamepad.getNumberedButton(8))
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
        
        this.drive.tankDrive(this.driverGamepad.getLeftY(), this.driverGamepad.getRightY());
    }
    
    private void controlArm() {
        if (this.counter % 2 == 0) {
            System.out.println("Current pot value: " + this.arm.getCurrentPosition());
            System.out.println("        set point: " + this.arm.getTargetPosition() + "\n");
        }

        int circleOffset = 0;
        if (this.operatorGamepad.getNumberedButton(5)) {
            circleOffset = 50;
        }
        
        if (this.operatorGamepad.getNumberedButton(1)){
            if(this.pickupButtonReleased) {
                if(!this.pickingUp) {
                    System.out.println("Starting pickup.");
                    this.arm.resetPIDInternals();
                    this.pickup();
                    this.cancellingPickup = false;
                    this.pickupButtonReleased = false;
                }
                else if(this.pickupButtonReleased && !this.cancellingPickup) {
                    System.out.println("cancel pickup");
                    this.cancelPickup();
                    this.arm.resetPIDInternals();
                    this.cancellingPickup = true;
                    this.pickupButtonReleased = false;
                }
            }
        } else {
            this.pickupButtonReleased = true;
            
            if(this.operatorGamepad.getNumberedButton(2)) {
                this.arm.resetPIDInternals();
                this.arm.setPosition(120 + circleOffset);
            }
            else if (this.operatorGamepad.getNumberedButton(3)) {
                this.arm.resetPIDInternals();
                this.arm.setPosition(250 + circleOffset);
            }
            else if (this.operatorGamepad.getNumberedButton(4)) {
                this.arm.resetPIDInternals();
                if(circleOffset > 0) {
                    this.arm.setPosition(435 + circleOffset - 5);
                }
                else {
                    this.arm.setPosition(435);
                }
                
            }
        }
        
        if(this.pickingUp) {
            this.pickup();
        }
        
        if(this.driverGamepad.getNumberedButton(10)) {
            this.minibot.deploy();
        }
    }
    
    private void controlClaw() {
        if(this.operatorGamepad.getNumberedButton(10)) {
            this.claw.setCanCloseJaw(true);
            this.claw.openJaw();
        }
        else {
            this.claw.closeJaw();
        }


//        if (this.operatorGamepad.getNumberedButton(5)) {
//            this.claw.pushOut();
        if (this.operatorGamepad.getNumberedButton(6)) {
            this.claw.turnUp();
        } else if (this.operatorGamepad.getNumberedButton(7)) {
            this.claw.pushOut();
        } else if (this.operatorGamepad.getNumberedButton(8)) {
            this.claw.turnDown();
        } else if (this.claw.isHoldingTube()) {
            this.claw.stop();
        } else {
            this.claw.pullIn();
        }
    }
    
    private void controlMinibot() {
        if(this.operatorGamepad.getDPad() == Gamepad.DPadDirection.kLeft) {
            System.out.println("minibot drop arm");
            this.minibot.dropArm();
        }
        else if(this.operatorGamepad.getDPad() == Gamepad.DPadDirection.kRight) {
            System.out.println("minibot reset arm");
            this.minibot.resetArm();
        }
    }

    private void pickup() {
        if (!this.pickingUp) {
            // start picking up
            this.pickingUp = true;

            this.pickupPosition = kOtherPickupPosition;
            this.arm.resetPIDInternals();
            this.arm.setPosition(130);

            System.out.println("Starting pickup, going to medium position");
        }

        if (this.pickupPosition == kOtherPickupPosition && this.arm.getCurrentPosition() >= 300 && this.arm.getCurrentPosition() <= 360) {
            // we've reached our medium position, drop the tower
            this.pickupPosition = kMediumPickupPosition;

            this.arm.resetPIDInternals();
            this.tower.lower();
            this.pickupTimer.start();

            System.out.println("Lowering tower");
        }

        if (this.pickupPosition == kMediumPickupPosition && this.pickupTimer.get() >= 0.2) {
            // we've moved the tower, go to the floor or hold position
            this.pickupPosition = kLowPickupPosition;

            this.arm.resetPIDInternals();
            this.pickupTimer.stop();
            this.pickupTimer.reset();

            System.out.println("Going to lower position");

            if (this.claw.isHoldingTube()) {
                this.arm.setPosition(35);
                // we're holding a tube in the lowest position, our work here is done
                this.pickingUp = false;

                System.out.println("I haz a tube");
            } else {
                this.arm.setPosition(95);
                System.out.println("I can haz tube?");
            }
        }

        if (this.pickupPosition == kLowPickupPosition && this.claw.isHoldingTube()) {
            // we've acquired the tube in the floor position, raise the tower
            this.pickupPosition = kMediumPickupPosition;

            this.tower.raise();
            this.pickupTimer.start();

            System.out.println("I have you now, raising tower");
        }
    }

    private void writeArmData() {
        final String output =
                (this.counter++) + "," +
                this.arm.getSetpoint() + "," +
                this.arm.getCurrentPosition() + "," +
                (Math.abs(this.arm.getCurrentSpeed()) < .001 ? 0 : this.arm.getCurrentSpeed());

        this.armDataFile.writeln(output);
        //System.out.println(output);
    }

    private void writeAutonData(final String out) {
//        this.autonDataFile.writeln("(" + System.currentTimeMillis() + "): " + out);
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
package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.can.CANTimeoutException;

public class Robot extends IterativeRobot {
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

    private DigitalInput leftLine, middleLine, rightLine;
    private Solenoid linePower;

    private Servo minibotServo;

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

        this.claw = new Claw(new DigitalInput(6), new Relay(1), new Relay(2), new Solenoid(5), new Solenoid(6));
        this.arm = new Arm(new AnalogChannel(3), topArmMotor, bottomArmMotor);
        this.tower = new Tower(new Solenoid(1), new Solenoid(2));
        this.drive = new Drive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor, new Solenoid(3), new Solenoid(4));

        this.compressor = new Compressor(5, 3);
        this.compressor.start();

        linePower = new Solenoid(8);
        linePower.set(true);
        this.leftLine = new DigitalInput(3);
        this.middleLine = new DigitalInput(2);
        this.rightLine = new DigitalInput(1);

        minibotServo = new Servo(1);

        System.out.println("Subsystems initialized");
        System.out.println("Robot initialized");
    }

    public void cancelPickup() {
        this.pickingUp = false;
        this.pickupPosition = kOtherPickupPosition;
        this.tower.raise();
        this.claw.closeJaw();
    }

    public void reset() {
//        this.counter = 0;
        this.shiftButtonPressed = false;
        this.pickingUp = false;
        this.pickupPosition = kOtherPickupPosition;

        this.drive.shiftUp();
        this.tower.raise();
        this.arm.resetPIDController();
        this.arm.resetSpeedLimiter();

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
    }

    // Autonomous mode
    public void autonomousInit() {
        this.reset();
        this.lineFollowReset();
        System.out.println("Raising arm.");
        this.arm.setPosition(615);
        atTee = false;
        drive.shiftUp();
    }

    public void autonomousContinuous() {
    }

    private Timer teaTimer = new Timer();   // After we see all 3 sensors on, wait to see if its a branch or the end
    private boolean atTee = false;          // True when we hit the end
    private Timer backAwayTimer = new Timer();  // Wait to back up after placing tube
    private double time = 0;

    public void autonomousPeriodic() {
        this.updateDashboard();
        this.writeArmData();
        
        if(teaTimer.get() > .1 && this.autoTSeen) {
            atTee = true;
            System.out.println("Tea timer expired - atTee true");
        }

        if(counter % 10 == 0)
            System.out.println("back away: " + backAwayTimer.get());
        if(atTee)
        {
            time = backAwayTimer.get();
            if(time == 0) {
                System.out.println("Starting back away timer");
                this.teaTimer.stop();
                this.claw.openJaw();
                this.claw.setCanCloseJaw(false);
                this.claw.turnDown();
                this.backAwayTimer.reset();
                this.backAwayTimer.start();
            }
            else if(time > .4 && time < .5) {
                System.out.println("Backing up");
                this.drive.tankDrive(-kAutoStraightSpeed, -kAutoStraightSpeed);
            }
            else if(time > .5 && time < 2) {
                // We've stopped - release the tube
                System.out.println("lowering arm & backing up");
                this.arm.setPosition(600);
                this.drive.tankDrive(-kAutoStraightSpeed, -kAutoStraightSpeed);
            }
            else if(time > 2) {
                System.out.println("Backed up. Stopping.");
                this.drive.tankDrive(0, 0);
            }
            else {
                this.drive.tankDrive(0, 0);
            }
        }
        else
        {
            this.followLine();
        }

        if(this.autoTSeen)
        {
            if(teaTimer.get() == 0) {
                System.out.println("Starting tea timer");
                teaTimer.reset();
                teaTimer.start();
            }
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
    }

    private boolean autoTSeen = false;
    private int currTurnDir = kNone;
    private Timer turnTimer = new Timer();
    private boolean firstTurn = true;      //True when we are doing the first turn to re-find the line

    private void lineFollowReset()
    {
        autoTSeen = false;
        currTurnDir = kNone;
        turnTimer.stop();
        turnTimer.reset();
        teaTimer.stop();
        teaTimer.reset();
        backAwayTimer.stop();
        backAwayTimer.reset();
        firstTurn = true;
    }

    private void followLine() {
        // Follow a line
        boolean left = leftLine.get();
        boolean middle = middleLine.get();
        boolean right = rightLine.get();

        autoTSeen = false;
        if(atTee) {
            if(counter % 10 == 0)
                System.out.println("Not moving - atTee true");
            this.drive.tankDrive(0,0);
        }
        else if(left && middle && right) {
            autoTSeen = true;
            if(!atTee) {
                System.out.println("Hunting for tee");
                this.drive.tankDrive(kAutoTurnSpeed, kAutoTurnSpeed);
            }
            else {
                //If all three are triggered, we have reached the T at the end of the line
                this.drive.tankDrive(0, 0);
                System.out.println("T seen - not moving.");
            }
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
            if (teaTimer.get() > 0 && lineLastSeen == kNone) {
                this.atTee = true;
                System.out.println("special Tee");
            }
            else if(currTurnDir == kNone) {
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
//        if (this.counter % 10 == 0) {
//            System.out.println("Current pot value: " + this.arm.getCurrentPosition());
//            System.out.println("        set point: " + this.arm.getTargetPosition() + "\n");
//        }

        if (this.rightGamepad.getNumberedButton(10)) {
            System.out.println("cancel pickup");
            this.cancelPickup();
            this.arm.resetPIDInternals();
        }
        else if(this.rightGamepad.getNumberedButton(1)) {
            this.arm.resetPIDInternals();
            this.pickup();
        }
        else if(this.pickingUp) {
            this.pickup();
        }
        else if(this.rightGamepad.getNumberedButton(2)) {
            this.arm.resetPIDInternals();
            this.arm.setPosition(330);
        }
        else if (this.rightGamepad.getNumberedButton(3)) {
            this.arm.resetPIDInternals();
            this.arm.setPosition(450);
        }
        else if (this.rightGamepad.getNumberedButton(4)) {
            this.arm.resetPIDInternals();
            this.arm.setPosition(615);
        }
    }
    
    private void controlClaw() {
        if(this.rightGamepad.getNumberedButton(7)) {
            this.claw.setCanCloseJaw(true);
            this.claw.openJaw();
        }
        else {
            this.claw.closeJaw();
        }


        if (this.rightGamepad.getNumberedButton(5)) {
            this.claw.pushOut();
        } else if (this.rightGamepad.getNumberedButton(6)) {
            this.claw.turnUp();
        } else if (this.rightGamepad.getNumberedButton(7)) {
            this.claw.pushOut();
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
            this.arm.resetPIDInternals();
            this.arm.setPosition(330);

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
                this.arm.setPosition(235);
                // we're holding a tube in the lowest position, our work here is done
                this.pickingUp = false;

                System.out.println("I haz a tube");
            } else {
                this.arm.setPosition(295);
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
                this.arm.getTargetPosition() + "," +
                this.arm.getCurrentPosition() + "," +
                (Math.abs(this.arm.getCurrentSpeed()) < .001 ? 0 : this.arm.getCurrentSpeed());

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
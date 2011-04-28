/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.Solenoid;

/**
 *
 * @author 1646
 */
public class Minibot {
    private final Solenoid deploy;
    private final Solenoid undeploy;
    private final Servo deployServo;
    
    public Minibot(Solenoid deploy, Solenoid undeploy, Servo deployServo) {
        this.deploy = deploy;
        this.undeploy = undeploy;
        this.deployServo = deployServo;
        
        this.undeploy();
        this.resetArm();
    }

    public final void undeploy() {
        this.deploy.set(false);
        this.undeploy.set(true);
    }

    public void deploy() {
        this.deploy.set(true);
        this.undeploy.set(false);
    }
    
    public void dropArm() {
        deployServo.set(1);
    }
    
    public void resetArm() {
        deployServo.set(0);
    }

}

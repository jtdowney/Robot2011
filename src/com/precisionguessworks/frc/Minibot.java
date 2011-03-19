/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.Solenoid;

/**
 *
 * @author 1646
 */
public class Minibot {
    private final Solenoid deploy;
    private final Solenoid undeploy;

    public Minibot(Solenoid deploy, Solenoid undeploy) {
        this.deploy = deploy;
        this.undeploy = undeploy;
        this.undeploy();
    }

    public final void undeploy() {
        this.deploy.set(false);
        this.undeploy.set(true);
    }

    public void deploy() {
        this.deploy.set(true);
        this.undeploy.set(false);
    }

}

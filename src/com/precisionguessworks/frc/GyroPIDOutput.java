package com.precisionguessworks.frc;

import edu.wpi.first.wpilibj.PIDOutput;

/**
 *
 * @author 1646
 */
public class GyroPIDOutput implements PIDOutput{

    private boolean goForward;  // If this is true, try to maintain 0 angle while moving forward
                                // Otherwise, simply try to turn to the angle
    private double value;


    public void pidWrite(double output) {
        value = output;
    }

    public double getValue()
    {
        return value;
    }
}

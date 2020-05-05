package stemsim.object;


import java.text.DecimalFormat;
import java.util.*;

import stemsim.event.SimulationEvent;
import stemsim.simulation.*;


/**
 * Interface for an object than can affect a stem cell including mutations
 * and crypts.
 *
 * The methods below return the $rate parameter affected by whatever the effect
 * is.  For no effect, simply return the $rate parameter unchanged.  To have
 * an affect, apply effect to the $rate parameter and return the computed value.
 * 
 * For example, deleterious mutation might compute an increased apoptotic effect
 * by multiplying the $rate by 1.1 and returning the result of the computation.
 *
 */
public interface StemCellAffector
{
    abstract public double apoptosisEffect(double $rate);
    
    abstract public double divisionEffect(double $rate);
    
    abstract public double mutationEffect(double $rate);
}

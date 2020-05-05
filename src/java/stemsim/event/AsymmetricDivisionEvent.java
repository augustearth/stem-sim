package stemsim.event;


import java.text.DecimalFormat;
import java.util.*;

import lib.KnuthRandom;
import lib.Probability;

import stemsim.object.*;
import stemsim.simulation.*;


/**
 * Represents a stem cell division.
 *
 */
public class AsymmetricDivisionEvent extends SimulationEvent 
implements Cloneable
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Generate a cell division for the given stem cell no sooner than given
     * start time and no later than given max time.
     *
     */
    public static AsymmetricDivisionEvent generate(double $currentTime, 
                                                   StemCell $stemCell)
    {
        Simulation sim = $stemCell.getSimulation();
        SimulationParams params = sim.getParams();
        double floor = params.getDouble("event.celldivision.floor");
        
        // calculate new event time -- unscaled
        double rate = $stemCell.getAsymmetricDivisionRate();

        double rnd = Probability.RandomFromExponential(rate, Simulation.RANDOM);
        double etime = $currentTime + rnd + floor;
        
        // DEBUG
        /*
        System.err.println("$currentTime = " + $currentTime);
        System.err.println("$stemCell.getDivisionRate() = " 
                           + $stemCell.getDivisionRate());
        System.err.println("asymmratio = " + asymmratio);
        System.err.println("rate = " + rate);
        System.err.println("rnd = " + rnd);
        System.err.println("etime = " + etime);
        if (Double.isNaN(etime)) System.exit(1);
         */
        // DEBUG
        
        // create new event
        AsymmetricDivisionEvent e = new AsymmetricDivisionEvent($stemCell);
        e.setTime(etime);
        e.setClocked($currentTime);

        // remove old event from queue
        EventQueue queue = $stemCell.getSimulation().getEventQueue();
        if ($stemCell.getAsymmetricDivisionEvent() != null)
        {
            $stemCell.getAsymmetricDivisionEvent().invalidate();
            queue.remove($stemCell.getAsymmetricDivisionEvent());
        }
        
        // add new event to the queue
        $stemCell.setAsymmetricDivisionEvent(e);
        queue.offer(e);
        
        // if the symmetric division event is within the min div time (12 hrs)
        // of the new asymm event, then push symm back min time
        $stemCell.correctDivEvents();
        
        return e;
    }
    
    
    /** static formatter for String output */
    static DecimalFormat dform = new DecimalFormat("#.###");
    
    
    static public void checkForTSGHit(StemCell $cell)
    {
        // check for a TSG hit
        // can either be in stem cell or transient chamber
        Simulation sim = $cell.getSimulation();
        SimulationParams params = sim.getParams();
        double pmut = $cell.getTSGMutationRate();
        
        // base probabilities of a TSG hit in the given location
        double pmutStem = pmut;
        double pmutTrans = 1 - 
            (Math.pow((1 - pmut), $cell.getTransientCells()));
        
        // flip the coin and check for mutation in stem cell
        double coin = Simulation.RANDOM.randomDouble();
        if (coin <= pmutStem)
        {
            stemCellHit($cell);
        }

        // flip the coin and check for mutation in TAC compartment
        coin = Simulation.RANDOM.randomDouble();
        if (coin <= pmutTrans)
        {
            transHit($cell);
        }
    }
    
    
    static void stemCellHit(StemCell $cell)
    {
        // DEBUG
        System.err.println("TSG StemCell");
        // DEBUG
        
        $cell.tsgHit();

        $cell.getCrypt().getTissue().recordTSGCell($cell);

        if ($cell.isCancerous())
        {
            cancer($cell);
        }
    }
    
    
    static void transHit(StemCell $cell)
    {
        $cell.tsgHitTAC();
        
        $cell.getCrypt().getTissue().recordTSGTAC($cell);
    }
    
    
    static void cancer(StemCell $cell)
    {
        // DEBUG
        System.err.println("CANCER IN " + $cell);
        // DEBUG
        
        Simulation sim = $cell.getSimulation();
        SimulationEndEvent se = SimulationEndEvent.create(sim);
        se.setTime(sim.getCurrentEvent().getTime());
        sim.getEventQueue().offer(se);
        return;
    }

    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** The stem cell that will divide. */
    StemCell _subject;
    
    
    /**
     * Constructor.
     *
     */
    public AsymmetricDivisionEvent(StemCell $subject)
    {
        _subject = $subject;
    }
    
    
    public StemCell getSubject()
    {
        return _subject;
    }
    
    
    /**
     * Perform this event -- create a daughter cell, generate a new mutation,
     * generate new stem cell events, register a change to the crypt.
     *
     */
    public void unfold()
    {
        // if somehow this event wants to happen for a dead StemCell, abort
        if (!_subject.isAlive())
        {
            return;
        }
        
        // check for a TSG hit
        checkForTSGHit(_subject);
        
        // check for fitness mutation
        SymmetricDivisionEvent.checkForMutation(_subject);
        
        // adjust transient cell count based on div rate?
        
        // generate new asymmetric div event
        _subject.setAsymmetricDivisionEvent(null);
        generate(getTime(), _subject);
    }
    
    
    
    /**
     * Return a copy of this object.  Shallow copy is OK.
     *
     */
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    
    /**
     * Return a String representation of this object.
     *
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("AsymmetricDivisionEvent(");
        if (!isValid())
        {
            buf.append("invalid ");
        }
        buf.append("cell:");
        buf.append(_subject.getId()); 
        buf.append(" t:");
        buf.append(TFORM.format(getTime())); 
        buf.append(" c:");
        buf.append(TFORM.format(_clocked)); 
        buf.append(" %:");
        buf.append(TFORM.format(_pctComplete)); 
        buf.append(")");
        return buf.toString();
    }
}

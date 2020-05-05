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
public class SymmetricDivisionEvent extends SimulationEvent
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Generate a cell division for the given stem cell no sooner than given
     * start time and no later than given max time.
     *
     */
    public static SymmetricDivisionEvent generate(double $currentTime, 
                                             StemCell $stemCell)
    {
        Simulation sim = $stemCell.getSimulation();
        SimulationParams params = sim.getParams();
        double floor = params.getDouble("event.celldivision.floor");
        
        // calculate new event time -- unscaled
        double rate = $stemCell.getDivisionRate();
        double rnd = Probability.RandomFromExponential(rate, Simulation.RANDOM);
        double etime = $currentTime + rnd + floor;
        
        // create new event
        SymmetricDivisionEvent e = new SymmetricDivisionEvent($stemCell);
        e.setTime(etime);
        e.setClocked($currentTime);

        // remove old event from queue
        EventQueue queue = $stemCell.getSimulation().getEventQueue();
        if ($stemCell.getDivisionEvent() != null)
        {
            $stemCell.getDivisionEvent().invalidate();
            queue.remove($stemCell.getDivisionEvent());
        }
        
        // add new event to the queue
        $stemCell.setDivisionEvent(e);
        queue.offer(e);
        
        // if the symmetric division event is within the min div time (12 hrs)
        // of the new asymm event, then push symm back min time
        $stemCell.correctDivEvents();
        
        return e;
    }
    
    
    /** static formatter for String output */
    static DecimalFormat dform = new DecimalFormat("#.###");

    // This could be made more efficient by checking for the time until the next
    // mutation event for separate mutation rates.
    static public void checkForMutation(StemCell $cell)
    {
        Simulation sim = $cell.getSimulation();
        if (Simulation.RANDOM.randomDouble() <= $cell.getMutationRate())
        {
            Mutation newMut = Mutation.createRandom(sim);
            $cell.addMutation(newMut);
        }
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
    public SymmetricDivisionEvent(StemCell $subject)
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
        
        // DEBUG
        if (_subject.isDoomed())
        {
            throw new RuntimeException("doomed cell dividing in " 
                               + _subject.getCrypt().getId()
                               + " at " + getTime());
        }
        // DEBUG
        
        // create daughter cell
        StemCell daughter = null;
        try
        {
            daughter = (StemCell)_subject.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException(e);
        }
        
        // add to crypt
        _subject.getCrypt().add(daughter);
        
        // conditionally generate new mutations
        checkForMutation(_subject);
        checkForMutation(daughter);
        AsymmetricDivisionEvent.checkForTSGHit(_subject);
        AsymmetricDivisionEvent.checkForTSGHit(daughter);

        // remove existing events
        _subject.clearEvents();
        daughter.clearEvents();
        
        // CryptChangeEvent.run() creates feedback and division events
        // have to create other events here
        ApoptosisEvent.generate(getTime(), _subject);
        ApoptosisEvent.generate(getTime(), daughter);
        AsymmetricDivisionEvent.generate(getTime(), _subject);
        AsymmetricDivisionEvent.generate(getTime(), daughter);
        
        // register the change to the crypt population
	    // This will generate new symmetric divisions and feedback apoptosis 
        // events
        CryptChangeEvent.run(_subject.getCrypt(), getTime());
    }
    
    
    /**
     * Return a String representation of this object.
     *
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("SymmetricDivisionEvent(");
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

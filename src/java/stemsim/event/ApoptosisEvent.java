package stemsim.event;


import java.util.PriorityQueue;

import lib.KnuthRandom;
import lib.Probability;

import stemsim.object.*;
import stemsim.simulation.*;


/**
 * An ApoptosisEvent object represents a stem cell apoptosis event.
 *
 */
public class ApoptosisEvent extends SimulationEvent implements Cloneable
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////

    public static ApoptosisEvent generate(double $currentTime,
                                          StemCell $cell)
    {
        Simulation sim = $cell.getSimulation();
        SimulationParams params = sim.getParams();
        double floor = params.getDouble("event.apoptosis.floor");

        double rate = $cell.getApoptosisRate();

        ApoptosisEvent old = $cell.getApoptosisEvent();
        ApoptosisEvent e = createEvent($currentTime, rate, floor, $cell, old);
        
        replace(sim, e, old);
        $cell.setApoptosisEvent(e);
        
        return e;
    }

    
    public static ApoptosisEvent generateFeedback(double $currentTime,
                                                  StemCell $cell)
    {
        Simulation sim = $cell.getSimulation();
        SimulationParams params = sim.getParams();
        double floor = params.getDouble("event.apoptosis.floor");
        
        // if have a dead neighbor, we don't generate an event (allow stem 
        // population to expand)
        // If a dead neighbor is replaced, feedback events will be generated 
        // next time there is a 
        // cryptChangeEvent (cell division, apoptosis, crypt bifurcation)
        if ($cell.getCrypt().hasDeadNeighbor())
        {
            removeFeedbackEvent($cell);
            return null;
        }

        // if we're not above the homeostatic level, we don't want an event
        // scheduled
        if (!$cell.getCrypt().isAboveHomeostatic())
        {
            removeFeedbackEvent($cell);
            return null;
        }
        
        double rate = $cell.getFeedbackApoptosisRate();

        ApoptosisEvent old = $cell.getFeedbackApoptosisEvent();
        ApoptosisEvent e = createEvent($currentTime, rate, floor, $cell, old);

        replace(sim, e, old);
        $cell.setFeedbackApoptosisEvent(e);
        
        return e;
    }
    
    
    static void removeFeedbackEvent(StemCell $cell)
    {
        EventQueue queue = $cell.getSimulation().getEventQueue();
        ApoptosisEvent old = $cell.getFeedbackApoptosisEvent();
        
        if (old != null)
        {
            old.invalidate();
            queue.remove(old);
            
            $cell.setFeedbackApoptosisEvent(null);
        }
    }
    
    
    static void replace(Simulation $sim,
                        ApoptosisEvent $new, 
                        ApoptosisEvent $old)
    {
        EventQueue queue = $sim.getEventQueue();

        // remove the old apoptosis event from the queue
        if ($old != null)
        {
            $old.invalidate();
            queue.remove($old);
        }

        // add new apoptosis event to the queue
        queue.offer($new);
    }

    
    static ApoptosisEvent createEvent(double $currentTime,
                                      double $apoptosisRate,
                                      double $floor,
                                      Cell $cell,
                                      ApoptosisEvent $existing)
    {
        Simulation sim = $cell.getSimulation();
        SimulationParams params = sim.getParams();
        
        // calculate event time -- unscaled
        double rate = $apoptosisRate;
        double rnd = Probability.RandomFromExponential(rate, Simulation.RANDOM);
        double etime = $currentTime + rnd + $floor;
        
        // create event
        ApoptosisEvent e = new ApoptosisEvent($cell);
        e.setTime(etime);

        // Noting when it was created so that we could figure out how much time 
        // has passed since the event was created
        // We used to use this to figure out how much of the process had occured
        // when we were rescaling time to the end of the event.
        // Don't think we use this anymore.
        e.setClocked($currentTime);

        return e;
    }
    
        
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    /** the subject of this event */
    Cell _subject;
    
    /** the crypt that the stem cell lives in which needs to be recorded
        for CryptStat purposes **/
    Crypt _crypt;
    
    /**
     * Constructor.
     *
     */
    public ApoptosisEvent(Cell $subject)
    {
        _subject = $subject;
    }
    
    
    /**
     * Perform this event -- kill the subject stem cell.
     *
     */
    public void unfold()
    {
        _crypt = _subject.getCrypt();

        // DEBUG
        /*
        StemCell sc = (StemCell)_subject;
        if (this == sc.getFeedbackApoptosisEvent())
        {
            System.err.println("feedback apop in " + _crypt.getId());
        }
        if (this == sc.getApoptosisEvent())
        {
            System.err.println("apop in " + _crypt.getId());
        }
         */
        // DEBUG
        
        // kill the subject and remove from crypt
        // subject = stem cell, killing it before removing is probably redundant
        _subject.kill();
        _crypt.remove(_subject);

        // remove lingering events from queue
        _subject.clearEvents();
        
        // register the change to the crypt population (not putting an event in 
        // the queue)
        // These used to go in the queue, but not any more.
        CryptChangeEvent.run(_crypt, getTime());
    }
    
    public Cell getSubject()
    {
        return _subject;
    }
    
    public Crypt getCrypt()
    {
        return _crypt;
    }
    
    /**
     * Return a copy of this object.  Shallow copy is OK.
     *
     */
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("ApoptosisEvent(");
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

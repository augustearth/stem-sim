package stemsim.event;


import java.text.DecimalFormat;
import java.util.*;

import lib.KnuthRandom;
import lib.Probability;

import stemsim.object.*;
import stemsim.simulation.Simulation;
import stemsim.simulation.EventQueue;


/**
 * Represents a crypt bifurcating -- half of its stem cells remaining, half
 * moving to a dead neighbor's crypt.
 *
 */
public class CryptBifurcationEvent extends SimulationEvent
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Formatter for text outout */
    static DecimalFormat dform = new DecimalFormat("#.###");

    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** The crypt that is going to bifurcate */
    Crypt _subject;
    
    /** The dead crypt that received living stem cells */
    Crypt _daughter;
    
    
    /**
     * Constructor.
     *
     */
    public CryptBifurcationEvent(Crypt $crypt)
    {
        _subject = $crypt;
    }
    
    
    /**
     * Return the crypt that gave up half its stem cells.
     *
     */
    public Crypt getSubject()
    {
        return _subject;
    }
    
    
    /**
     * Return the crypt that received stem cells.
     *
     */
    public Crypt getDaughter()
    {
        return _daughter;
    }
    
    
    /**
     * Perform this event.
     *
     */
    public void unfold()
    {
        if (!_subject.isAlive())
        {
            return;
        }
        
        // look for dead neighbors
        List<Crypt> deadneighbors = _subject.deadNeighbors();
        
        // if no place to go, no-op
        if (deadneighbors.size() == 0)
        {
            return;
        }

        // pick new digs
        KnuthRandom rnd = Simulation.RANDOM;
        int numdead = deadneighbors.size();
        int n = rnd.randomInt(numdead);
        if (n > numdead - 1)
        {
            n = numdead - 1;
        }
        _daughter = deadneighbors.get(n);
        
        // bifurcate
        int numtomove = _subject.getStemCells().size();
        numtomove = Math.round(numtomove/2);
        StemCell[] cells = new StemCell[_subject.getStemCells().size()];
        cells = _subject.getStemCells().toArray(cells);
        for (int i=0; i<numtomove; i++)
        {
            StemCell cell = cells[i];
            _subject.remove(cell);
            _daughter.add(cell);
        }
        
        // register the change to the crypt population
        CryptChangeEvent.run(_subject, getTime());
        CryptChangeEvent.run(_daughter, getTime());
        
        // DEBUG
        System.err.println("bifurcation: " 
                           + _subject.getId() 
                           + " -> " 
                           + _daughter.getId());
        // DEBUG
    }
    
    
    /**
     * Return a text representation of this event.
     *
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("CryptBifurcationEvent(");
        if (!isValid())
        {
            buf.append("invalid ");
        }
        buf.append("t:");
        buf.append(TFORM.format(getTime())); 
        buf.append(" crypt:");
        buf.append(_subject.getId()); 
        buf.append(")");
        return buf.toString();
    }
    
}

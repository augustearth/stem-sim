package stemsim.simulation;


import java.io.*;
import java.text.*;
import java.util.*;

import stemsim.event.*;
import stemsim.object.*;
import stemsim.stat.*;


/**
 * Simulate a tissue of crypts.
 *
 */
public class TissueSimulation extends Simulation
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    /** formatter for text output */
    static DecimalFormat tform = new DecimalFormat("#.#");

    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    /** The tissue of this simulation */
    Tissue _tissue;
    
    
    /**
     * Initialize this simulation by creating the Tissue object.
     *
     */
    public void init(SimulationParams $params)
    {
        _tissue = Tissue.create(this);
        
        if ($params.isEnabled("fixationstat"))
            registerListener(new FixationStat());

        if ($params.isEnabled("cryptstat"))
            registerListener(new CryptStat());
        
        if ($params.isEnabled("stempopstat"))
            registerListener(new StemPopStat());

        if ($params.isEnabled("divstat"))
            registerListener(new DivStat());

        if ($params.isEnabled("tissuepopstat"))
            registerListener(new TissuePopStat());
    }

    
    /**
     * Return true if this simulation can continue -- can do so as long as the
     * tissue is alive.
     *
     */
    public boolean canContinue()
    {
        return _tissue.isAlive();
    }

    
    /**
     * Return the tissue of this simulation.
     *
     */
    public Tissue getTissue()
    {
        return _tissue;
    }
    
    
    /**
     * Print debug state -- time of last event, number of events in the queue,
     * and the tissue.
     *
     */
    public void debugPrintState()
    {
        SimulationEvent last = getLastEvent();
        if (last != null)
        {
            System.out.println("Time:" + tform.format(last.getTime()));
            // System.out.println("Event:" + last);
            System.out.println("Queue:" + getEventQueue().size());
        }
        System.out.println(_tissue);
    }
    
    
    /**
     * Return an XML representation of this object.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<tissuesim>");
        pw.println();
        
        pw.println("<lastevent>");
        if (getLastEvent() != null)
        {
            pw.println(getLastEvent().toXML());
        }
        pw.println("</lastevent>");
        pw.println();
        
        pw.println(getTissue().toXML());
        pw.println();

        pw.println("</tissuesim>");
        pw.println();
        
        return sw.toString();
    }
}

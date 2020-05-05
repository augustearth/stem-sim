package stemsim.event;


import java.text.DecimalFormat;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.PriorityQueue;

import stemsim.simulation.Simulation;


/**
 * Superclass for all simulation events.
 *
 */
public abstract class SimulationEvent
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** static formatter for string output */
    final static DecimalFormat TFORM = new DecimalFormat("#.#");

    
    /**
     * Comparator that orders SimulationEvents by time.
     *
     */
    public static class TimeComparator implements Comparator<SimulationEvent>
    {
        public int compare(SimulationEvent $o1, SimulationEvent $o2)
        {
            if ($o1 == $o2)
            {
                return 0;
            }
            
            double t1 = $o1.getTime();
            double t2 = $o2.getTime();
            if (t1 > t2)
            {
                return 1;
            }
            else if (t1 < t2)
            {
                return -1;
            }

            // luck of the draw, y'all
            // can't use 0, or events will get filtered out by stupid java
            return 1;
        }
    }

    
    /**
     * Scales a newly generated event execution time based on the original 
     * event.
     *
     */
    static public void scale(SimulationEvent $org, SimulationEvent $event)
    {
        if ($org == null || !$org.isValid())
        {
            return;
        }

        // check to see of the org pct complete is 1.0
        // really, it's not 1.0, but close enough that it's beyond the precision
        // of a java double
        // in this case, we just go with the old event times since the event
        // is going to happen inside our immediate time horizon
        if ($org._pctComplete == 1.0)
        {
            $event.setTime($org.getTime());
            $event.setClocked($org.getClocked());
            $event.setPercentComplete($org.getPercentComplete());
            return;
        }

        double originalWaitTime = ($org._time - $org._clocked)
                                / (1.0 - $org._pctComplete);
        checkForNaN(originalWaitTime, "originalWaitTime", $org, $event);
        
        double origin = $org._time - originalWaitTime;
        checkForNaN(origin, "origin", $org, $event);

        double curTime = $event._clocked;
        double pctComp = (curTime - origin) / originalWaitTime;
        checkForNaN(pctComp, "pctComp", $org, $event);
        

        double waitTime = $event.getTime() - curTime;
        waitTime *= (1.0 - pctComp);
        checkForNaN(waitTime, "waitTime", $org, $event);
        
        $event.setTime(curTime + waitTime);
        
        pctComp = ($event._clocked - origin)
                / ($event.getTime() - origin); 
        checkForNaN(pctComp, "pctComp2", $org, $event);

        $event._pctComplete = pctComp;
        
        if ($event.getTime() < $event._clocked) 
        {
            System.err.println("WARNING: going back in time: " + $event); 
            //throw new RuntimeException("going back in time: " + $event);
        }
    }
    
    
    static void checkForNaN(double $val, 
                            String $label,
                            SimulationEvent $org,
                            SimulationEvent $event)
    {
        if (Double.isNaN($val))
        {
            System.err.println("NAN: " + $label);
            System.err.println("$org: " + $org);
            System.err.println("$event: " + $event);
            System.exit(1);
        }
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////

    /** The time this event is to unfold */
    double _time;
    
    /** Flag for event validity -- always true at outset */
    boolean _valid = true;
    
    /** The time this event was clocked -- used in scaling */
    double _clocked = 0.0;
    
    /** Percentage completion at clocked time -- used in scaling */
    double _pctComplete = 0.0;
    
    
    /** 
     * Execute this event 
     *
     */
    abstract public void unfold();

    
    /**
     * Invalidate this event.
     *
     */
    public void invalidate()
    {
        _valid = false;
    }
    
    
    /**
     * Return true if this event is still valid.
     *
     */
    public boolean isValid()
    {
        return _valid;
    }
    
    
    /**
     * Set the time this event is to execute.
     *
     */
    public void setTime(double $time)
    {
        _time = $time;
    }
    
    
    /**
     * Get the time this event is to execute.
     *
     */
    public double getTime()
    {
        return _time;
    }

    
    /**
     * Set the time this event was clocked.
     *
     */
    public void setClocked(double $time)
    {
        _clocked = $time;
    }
    
    
    /**
     * Get the time this event was clocked.
     *
     */
    public double getClocked()
    {
        return _clocked;
    }

    
    public void setPercentComplete(double $time)
    {
        _pctComplete = $time;
    }
    
    
    public double getPercentComplete()
    {
        return _pctComplete;
    }
    
    
    /**
     * Return an XML representation of this object.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<event>");
        
        pw.println("<type>");
        pw.println(getClass().getName());
        pw.println("</type>");
        
        pw.println("<valid>");
        pw.println(_valid);
        pw.println("</valid>");
        
        pw.println("<time>");
        pw.println(_time);
        pw.println("</time>");
        
        pw.println("</event>");
        
        pw.flush();
        return sw.toString();
    }
    
    public String toString()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println(getClass().getName());
        pw.println("t = " + getTime());
        pw.println("c = " + _clocked);
        pw.println("% = " + _pctComplete);
        
        pw.flush();
        return sw.toString();
    }
}

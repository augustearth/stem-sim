package stemsim.event;


import java.io.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import stemsim.simulation.*;
import stemsim.object.*;
import stemsim.event.*;


public class SimulationEventTest extends TestCase 
{
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    

    public void testScale() throws IOException
    {
        SimulationEvent org = null;
        SimulationEvent e = null;

        org = new DummyEvent();
        org.setTime(10.0);
        org._clocked = 0.0;
        org._pctComplete = 0.0;
        
        // shorten wait time
        e = new DummyEvent();
        e.setTime(10.0);
        e._clocked = 5.0;
        e._pctComplete = 0.0;
        
        SimulationEvent.scale(org, e);
        assertEquals(7.5, e.getTime());
        assertEquals(5.0, e._clocked);
        assertEquals(5.0/7.5, e._pctComplete);

        // on the nose
        e.setTime(15.0);
        e._clocked = 5.0;
        e._pctComplete = 0.0;
        
        SimulationEvent.scale(org, e);
        assertEquals(10.0, e.getTime());
        assertEquals(5.0, e._clocked);
        assertEquals(0.5, e._pctComplete);

        // lengthen wait time
        e.setTime(25.0);
        e._clocked = 5.0;
        e._pctComplete = 0.0;
        
        SimulationEvent.scale(org, e);
        assertEquals(15.0, e.getTime());
        assertEquals(5.0, e._clocked);
        assertEquals(5.0/15.0, e._pctComplete);
        
        // non-zero origin
        org = new DummyEvent();
        org.setTime(16.0);
        org._clocked = 10.0;
        org._pctComplete = 0.0;

        e.setTime(14.0);
        e._clocked = 13.0;
        e._pctComplete = 0.0;

        SimulationEvent.scale(org, e);
        assertEquals(13.5, e.getTime());
        assertEquals(13.0, e._clocked);
        assertEquals(3.0/3.5, e._pctComplete);
    }

    
    /*
	public void testPercentComplete() throws IOException
    {
        SimulationEvent e = new DummyEvent();
        e.setTime(10.0);
        e._clocked = 0.0;
        e._pctComplete = 0.0;
        
        assertEquals(0.0, SimulationEvent.percentComplete(e, 0.0));
        assertEquals(0.25, SimulationEvent.percentComplete(e, 2.5));
        assertEquals(0.50, SimulationEvent.percentComplete(e, 5.0));
        assertEquals(0.75, SimulationEvent.percentComplete(e, 7.5));
        assertEquals(1.0, SimulationEvent.percentComplete(e, 10.0));
        
        e._clocked = 5.0;
        e._pctComplete = 0.5;
        assertEquals(0.0, SimulationEvent.percentComplete(e, 0.0));
        assertEquals(0.25, SimulationEvent.percentComplete(e, 2.5));
        assertEquals(0.50, SimulationEvent.percentComplete(e, 5.0));
        assertEquals(0.75, SimulationEvent.percentComplete(e, 7.5));
        assertEquals(1.0, SimulationEvent.percentComplete(e, 10.0));
	}
     */


    
    class DummyEvent extends SimulationEvent
    {
        public void unfold()
        {
        }
    }

}
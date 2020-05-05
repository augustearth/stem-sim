package stemsim.simulation;


import java.io.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import stemsim.simulation.*;
import stemsim.object.*;
import stemsim.event.*;


public class EventQueueTest extends TestCase 
{
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    
    public void testRemove() throws Exception
    {
        SimulationEvent e1 = new DummyEvent();
        e1.setTime(1.0);

        SimulationEvent e2 = new DummyEvent();
        e2.setTime(1.0);
        
        EventQueue q = new EventQueue();
        assertEquals(0, q.size());

        q.offer(e1);
        assertEquals(1, q.size());

        q.offer(e2);
        assertEquals(2, q.size());

        assertTrue(q.remove(e1));
        assertEquals(1, q.size());
        
        SimulationEvent e = q.poll();
        assertEquals(e2, e);

        // remove the 1st offered
        for (int i=0; i<100; i++)
        {
            q.offer(e1);
            q.offer(e2);
            assertEquals(2, q.size());

            assertTrue(q.remove(e1));
            assertEquals(1, q.size());

            e = q.poll();
            assertEquals(e2, e);
        }
        
        // remove the 2nd offered
        for (int i=0; i<100; i++)
        {
            q.offer(e1);
            q.offer(e2);
            assertEquals(2, q.size());
            
            assertTrue(q.remove(e2));
            assertEquals(1, q.size());
            
            e = q.poll();
            assertEquals(e1, e);
        }
    }

    
    class DummyEvent extends SimulationEvent
    {
        public void unfold()
        {
        }
    }

}
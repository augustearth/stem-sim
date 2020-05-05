package stemsim.simulation;


import java.util.*;
import stemsim.event.*;


/**
 * An event queue for a simultion, built around the Java PriorityQueue object.
 *
 */
public class EventQueue
{
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Comparator to order events by time */
    Comparator _eComp = new SimulationEvent.TimeComparator();
    
    /** PriorityQueue containing all events */
    PriorityQueue<SimulationEvent> _eventQueue = 
        new PriorityQueue<SimulationEvent>(1000, _eComp);
    
    
    /**
     * Poll for the next event.
     *
     */
    public SimulationEvent poll()
    {
        return _eventQueue.poll();
    }

    
    /**
     * Peek at the next event.
     *
     */
    public SimulationEvent peek()
    {
        return _eventQueue.peek();
    }
    
    
    
    /**
     * Add an event to this queue.
     *
     */
    public boolean offer(SimulationEvent $event)
    {
        if (Double.isNaN($event.getTime()))
        {
            System.err.println("NAN: " + $event);
            System.exit(1);
        }
        
        return _eventQueue.offer($event);
    }
    
    
    /**
     * Remove an event from the queue.
     *
     */
    public boolean remove(SimulationEvent $event)
    {
        return _eventQueue.remove($event);
    }
    
    
    /**
     * Return the size of the queue.
     * 
     */
    public int size()
    {
        return _eventQueue.size();
    }
    
    
    /**
     * Return string representation of the queue.
     *
     */
    public String toString()
    {
        return _eventQueue.toString();
    }
}

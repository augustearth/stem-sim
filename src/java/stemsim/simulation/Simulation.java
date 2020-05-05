package stemsim.simulation;


import java.io.*;
import java.util.*;

import lib.KnuthRandom;

import stemsim.event.*;


/**
 * Superclass for simulations that can run in this framework.
 *
 */
public abstract class Simulation
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** unique identifier for simulation objects */
    static int ID = 1;
    static int nextId()
    {
        return ID++;
    }
    
    /** random number generator for all simulations */
    static final public KnuthRandom RANDOM = new KnuthRandom();
    static
    {
        RANDOM.seedRandom(-1);
    }
    

    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    /** unique identifier for this simulation object */
    int _id = nextId();
    
    /** optional label for this simulation */
    String _label = "";
    
    /** the simulation parameters of this object */
    SimulationParams _params = null;
    
    /** the single event queue for this simulation */
    EventQueue _eventQueue = new EventQueue();
    
    /** the maximum duration of this simulation */
    double _duration = -1;
    
    /** the last event executed by this simulation */
    SimulationEvent _last;
    
    /** the current event being executed */
    SimulationEvent _current;
    
    /** list of listeners of this simulation's events */
    Set<SimEventListener> _listeners = new HashSet<SimEventListener>();
    
    /** the output directory for result files */
    File _outputDirectory = null;
    
    
    /**
     * Initialize this simulation object given parameters -- called before the
     * run method.
     *
     */
    abstract public void init(SimulationParams $params) throws Exception;
    
    
    /**
     * Return the last event executed by this simulation.
     *
     */
    public SimulationEvent getLastEvent()
    {
        return _last;
    }

    /**
     * Return the current event being executed by this simulation.
     *
     */
    public SimulationEvent getCurrentEvent()
    {
        return _current;
    }
    
    
    /**
     * Print the current debug state.
     *
     */
    abstract public void debugPrintState();
    
    
    /**
     * Return true if this simulation can(should) continue;
     *
     */
    abstract public boolean canContinue();
    
    
    /**
     * Return an XML representation of this simulation object.
     *
     */
    abstract public String toXML();
    
    
    /**
     * Return the unique identifier of this simulation object.
     *
     */
    public String getId()
    {
        String ids = String.valueOf(_id);
        if (_id < 10)
        {
            ids = "0" + ids;
        }
        return ids;
    }
    
    
    /**
     * Set the output directory to which stat files will be written.
     *
     */
    public void setOutputDirectory(File $file)
    {
        _outputDirectory = $file;
    }
    
    
    /**
     * Get the output directory.
     *
     */
    public File getOutputDirectory()
    {
        return _outputDirectory;
    }
    
    
    /**
     * Return the event queue of this simulation.
     *
     */
    public EventQueue getEventQueue()
    {
        return _eventQueue;
    }
    

    public void setLabel(String $label)
    {
        _label = $label;
    }
    
    public String getLabel()
    {
        return _label;
    }
    
    
    /**
     * Set this simulation's params.
     *
     */ 
    public void setParams(SimulationParams $params)
    {
        _params = $params;
    }
    
    
    /**
     * Get this simulation's params.
     *
     */
    public SimulationParams getParams()
    {
        return _params;
    }
    
    
    /**
     * Register a listener of this simulation's events.
     *
     */
    public void registerListener(SimEventListener $listener)
    {
        if (!_listeners.contains($listener))
        {
            _listeners.add($listener);
        }
    }
    
    
    /**
     * Run this simulation.
     *
     */
    public final void run() throws Exception
    {
        _eventQueue = new EventQueue();
        _duration = getParams().getDouble("simulation.duration");
        init(getParams());
        
        execute();
    }
    
    
    /**
     * Execute this simulation
     *
     */
    public void execute() throws Exception
    {
        // gather debug settings
        SimulationParams params = getParams();
        int debugStep = 0;
        long debugTime = 0;
        String debugStepType = params.getString("simulation.debug.echo.time");
        boolean debug = params.getBoolean("simulation.debug.echo");
        if (debug)
        {
            debugStep = params.getInt("simulation.debug.echo.step");
            if (debugStepType.equals("real"))
            {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.SECOND, debugStep);
                debugTime = c.getTimeInMillis();
            }
            else if (debugStepType.equals("event"))
            {
                debugTime = debugStep;
            }
            else
            {
                throw new IllegalArgumentException(
                                "Invalid simulation.debug.echo.time");
            }
        }
        int debugMaxEvents = params.getInt("simulation.debug.maxevents");
        int count = 0; // The number of events processed
        
        // front load SimulationStartEvent
        getEventQueue().offer(new SimulationStartEvent(this));
        
        // echo initial state
        if (debug) debugPrintState();
        
        // run the simluation
        while ((_current = getEventQueue().poll()) != null && canContinue())
        {
            // break on simulationend event
            if (_current instanceof SimulationEndEvent)
            {
                break;
            }
            
            // process event only if valid
            // We try to remove all invalid events (which happens often due to 
            // homeostasis in the stem compartment)
            // So this should always be valid but we are just making sure here.
            if (_current.isValid())  
            {
                // DEBUG
                if (Double.isNaN(_current.getTime()))
                {
                    // Just logging in case something went wrong (doesn't happen
                    // anymore)
                    System.err.println("NAN: " + _current); 
                }
                // DEBUG
                
                _current.unfold();
                _last = _current;
                count++;
            }
            
            // notify listeners
            for (SimEventListener el : _listeners)
            {
                el.notify(_current);
            }
            
            // stop if we're past our max scheduled duration
            if (_duration > 0 && _current.getTime() > _duration)
            {
                break;
            }
            
            // stop if we've exceeded max events
            if (debugMaxEvents > 0 && count >= debugMaxEvents)
            {
                break;
            }
            
            // echo state
            if (debug)
            {
                Calendar c = Calendar.getInstance();
                if (debugStepType.equals("real") 
                    && c.getTimeInMillis() > debugTime)
                {
                    debugPrintState();
                    c.add(Calendar.SECOND, debugStep);
                    debugTime = c.getTimeInMillis();
                }
                else if (debugStepType.equals("event") 
                         && _current.getTime() > debugTime)
                {
                    debugPrintState();
                    debugTime += debugStep;
                }
            }
            
            // give us a chance to break out via a 'q' in the standard in
            if (count % 100 == 0)
            {
                int n = System.in.available();
                if (n > 0)
                {
                    byte[] bytes = new byte[n];
                    System.in.read(bytes);
                    String s = new String(bytes);
                    if (s.contains("q"))
                    {
                        break;
                    }
                }
            }
        }
        
        // notify listeners that we're done
        SimulationEndEvent se = (_current instanceof SimulationEndEvent)
            ? (SimulationEndEvent)_current
            : SimulationEndEvent.create(this);
        for (SimEventListener el : _listeners)
        {
            el.notify(se);
        }
        
        // echo final state
        if (debug) debugPrintState();
    }
    
}

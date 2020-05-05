package stemsim.event;


import stemsim.simulation.Simulation;


/**
 * Event used to register the start of a simulation.
 *
 */
public class SimulationStartEvent extends SimulationEvent
{
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    Simulation _subject;
    
    public SimulationStartEvent(Simulation $simulation)
    {
        _subject = $simulation;
        setTime(-1.0);
    }
    
    public Simulation getSubject()
    {
        return _subject;
    }
    
    public void unfold()
    {
        // no-op
    }
}

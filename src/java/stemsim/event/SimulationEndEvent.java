package stemsim.event;


import stemsim.simulation.Simulation;


/**
 * Event used to register the end of the simulation.
 *
 */
public class SimulationEndEvent extends SimulationEvent
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    public static SimulationEndEvent create(Simulation $simulation)
    {
        SimulationEndEvent se = new SimulationEndEvent($simulation);
        if ($simulation.getLastEvent() != null)
        {
            se.setTime($simulation.getLastEvent().getTime());
        }
        return se;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    Simulation _subject;
    
    private SimulationEndEvent(Simulation $simulation)
    {
        _subject = $simulation;
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

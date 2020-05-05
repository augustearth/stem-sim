package stemsim.object;


import stemsim.simulation.Simulation;


/**
 * Superclass for all objects that exist within a simulation.
 *
 */
public abstract class SimulationObject implements Cloneable
{
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    Simulation _simulation = null;
    
    SimulationObject(Simulation $simulation)
    {
        setSimulation($simulation);
    }
    
    public void setSimulation(Simulation $simulation)
    {
        _simulation = $simulation;
    }
    
    public Simulation getSimulation()
    {
        return _simulation;
    }
    
    /**
     * Return a copy of this object.  Shallow copy is OK.
     *
     */
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}

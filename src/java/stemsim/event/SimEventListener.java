package stemsim.event;


/**
 * An interface for classes that listen to simulation events for the purpose
 * of computing statistics.
 *
 */
public interface SimEventListener
{
    
    /**
     * Notify the listener of the simulation event.
     *
     */
    abstract public void notify(SimulationEvent $event);
}
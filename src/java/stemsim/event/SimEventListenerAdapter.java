package stemsim.event;


/** 
 * An adapter class for the interface SimEventListener that provides
 * convenience methods for selected events.
 *
 */
public abstract class SimEventListenerAdapter implements SimEventListener
{

    public void notify(SimulationEvent $event)
    {
        if ($event instanceof SimulationStartEvent)
        {
            start((SimulationStartEvent)$event);
        }

        if ($event instanceof SymmetricDivisionEvent)
        {
            division((SymmetricDivisionEvent)$event);
        }
        
        if ($event instanceof AsymmetricDivisionEvent)
        {
            asymmetricDivision((AsymmetricDivisionEvent)$event);
        }
        
        if ($event instanceof ApoptosisEvent)
        {
            apoptosis((ApoptosisEvent)$event);
        }
        
        if ($event instanceof SimulationEndEvent)
        {
            end((SimulationEndEvent)$event);
        }

        if ($event instanceof CryptBifurcationEvent)
        {
            bifurcation((CryptBifurcationEvent)$event);
        }
    }
    
    public void division(SymmetricDivisionEvent $event) 
    {
        
    }
    
    public void asymmetricDivision(AsymmetricDivisionEvent $event) 
    {
        
    }

    public void apoptosis(ApoptosisEvent $event) 
    {
        
    }
    
    public void bifurcation(CryptBifurcationEvent $event) 
    {
        
    }
    
    public void end(SimulationEndEvent $event) 
    {
        
    }

    public void start(SimulationStartEvent $event) 
    {
        
    }
    
}
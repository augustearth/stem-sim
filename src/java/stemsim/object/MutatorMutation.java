package stemsim.object;


import java.util.*;

import stemsim.simulation.*;


/**
 * Represents a mutator mutation.
 *
 */
public class MutatorMutation extends Mutation
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    /** map of singleton mutation objects */
    static Map<Simulation,MutatorMutation> muts = 
        new HashMap<Simulation,MutatorMutation>();
    
    /** create a (singleton) mutation object */
    static public MutatorMutation create(Simulation $simulation)
    {
        MutatorMutation mut = muts.get($simulation);
        if (mut == null)
        {
            mut = new MutatorMutation($simulation);
            mut.setLabel("singleton");

            muts.put($simulation, mut);
        }
        return mut;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    double _multiplier;
    
    private MutatorMutation(Simulation $simulation)
    {
        super($simulation);

        SimulationParams params = getSimulation().getParams();
        _multiplier = params.getDouble("mutatormutation.mutation.multiplier");
    }
    
    public double mutationEffect(double $rate)
    {
        return $rate * _multiplier;
    }
    
    public String toString()
    {
        return "m";
    }
}

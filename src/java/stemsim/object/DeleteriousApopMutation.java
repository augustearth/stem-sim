package stemsim.object;


import java.util.*;

import stemsim.simulation.*;


/**
 * Represents a deleterious mutation that increases the apoptosis rate of a
 * stem cell.
 *
 */
public class DeleteriousApopMutation extends Mutation
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** map of singleton mutation objects per simulation */
    static Map<Simulation,DeleteriousApopMutation> muts = 
        new HashMap<Simulation,DeleteriousApopMutation>();
    
    
    /** creates a (singleton) mutation object */
    static public DeleteriousApopMutation create(Simulation $simulation)
    {
        DeleteriousApopMutation mut = muts.get($simulation);
        
        // This makes sure this is only done once
        if (mut == null) 
        {
            // we could make different types of deleterious mutations but we are
            // only using one type
            mut = new DeleteriousApopMutation($simulation);
            
            mut.setLabel("singleton"); 

            muts.put($simulation, mut);
        }
        return mut;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    double _multiplier;
    
    private DeleteriousApopMutation(Simulation $simulation)
    {
        super($simulation);

        SimulationParams params = getSimulation().getParams();
        _multiplier = 
            params.getDouble("deleteriousmutation.apoptosis.multiplier");
    }
    
    public double apoptosisEffect(double $rate)
    {
        return $rate * _multiplier;
    }
    
    public String toString()
    {
        return "da";
    }
    
}

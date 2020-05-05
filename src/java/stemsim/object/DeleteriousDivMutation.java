package stemsim.object;


import java.util.*;

import stemsim.simulation.*;


/**
 * Represents a deleterious mutation that decreases the division rate of a 
 * stem cell.
 *
 */ 
public class DeleteriousDivMutation extends Mutation
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Map of singleton mutation objects */
    static Map<Simulation,DeleteriousDivMutation> muts = 
        new HashMap<Simulation,DeleteriousDivMutation>();
    
    
    /** Create a (singleton) mutation object*/
    static public DeleteriousDivMutation create(Simulation $simulation)
    {
        DeleteriousDivMutation mut = muts.get($simulation);
        if (mut == null)
        {
            mut = new DeleteriousDivMutation($simulation);
            mut.setLabel("singleton");
            
            muts.put($simulation, mut);
        }
        return mut;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    double _divMultiplier;
    
    private DeleteriousDivMutation(Simulation $simulation)
    {
        super($simulation);

        SimulationParams params = getSimulation().getParams();
        _divMultiplier = 
            params.getDouble("deleteriousmutation.division.multiplier");
    }
    
    public double divisionEffect(double $rate)
    {
        return $rate * _divMultiplier;
    }
    
    public String toString()
    {
        return "dd";
    }
}

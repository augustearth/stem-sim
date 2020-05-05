package stemsim.object;


import java.util.*;

import stemsim.simulation.*;


/**
 * Represents a beneficial mutation that increases the division rate of a 
 * StemCell.
 *
 */ 
public class BeneficialDivMutation extends Mutation
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Map of singleton mutation objects */
    static Map<Simulation,BeneficialDivMutation> muts = 
        new HashMap<Simulation,BeneficialDivMutation>();
    
    
    /** Create a (singleton) mutation object*/
    static public BeneficialDivMutation create(Simulation $simulation)
    {
        BeneficialDivMutation mut = muts.get($simulation);
        if (mut == null)
        {
            mut = new BeneficialDivMutation($simulation);
            mut.setLabel("singleton");
            
            muts.put($simulation, mut);
        }
        return mut;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    double _divMultiplier;
    
    private BeneficialDivMutation(Simulation $simulation)
    {
        super($simulation);

        SimulationParams params = getSimulation().getParams();
        _divMultiplier = 
            params.getDouble("beneficialmutation.division.multiplier");
    }
    
    public double divisionEffect(double $rate)
    {
        return $rate * _divMultiplier;
    }
    
    public String toString()
    {
        return "bd";
    }
}

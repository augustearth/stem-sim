package stemsim.object;


import java.util.*;

import stemsim.simulation.*;


/**
 * A beneficial mutation that reduces the apoptosis rate of a stem cell.
 *
 */
public class BeneficialApopMutation extends Mutation
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** a Map of Simulation -> singleton BeneficialApopMutation objects */
    static Map<Simulation,BeneficialApopMutation> muts = 
        new HashMap<Simulation,BeneficialApopMutation>();
    
    /** create a (singleton) BeneficialApopMutation for the given sim */
    static public BeneficialApopMutation create(Simulation $simulation)
    {
        BeneficialApopMutation mut = muts.get($simulation);
        if (mut == null)
        {
            mut = new BeneficialApopMutation($simulation);
            mut.setLabel("singleton");
            
            muts.put($simulation, mut);
        }
        
        return mut;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** the factor to multiply to the stem cell's apoptosis rate */
    double _apopMultiplier;
    
    private BeneficialApopMutation(Simulation $simulation)
    {
        super($simulation);

        SimulationParams params = getSimulation().getParams();
        _apopMultiplier = 
            params.getDouble("beneficialmutation.apoptosis.multiplier");
    }
    
    public double apoptosisEffect(double $rate)
    {
        return $rate * _apopMultiplier;
    }
    
    public String toString()
    {
        return "ba";
    }
}

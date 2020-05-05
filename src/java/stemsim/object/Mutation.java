package stemsim.object;


import java.io.StringWriter;
import java.io.PrintWriter;

import stemsim.simulation.*;


/**
 * Superclass for stem cell fitness mutation objects.
 *
 * Fitness mutations can affect stem cells as per the StemCellAffector
 * interface -- base apoptosis, base division, and mutation rates.  This 
 * superclass does not have any effect and should be sub-classed to create
 * mutations that have fitness effects.
 *
 * It is expected that each cell will store its mutation objects in a list and
 * iterate over that list applying the effect of each mutation when calculating
 * the affected rates.
 *
 */
public class Mutation extends SimulationObject implements StemCellAffector
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Creates a random mutation based on the mutation ratio parameters.
     *
     */
    public static Mutation createRandom(Simulation $simulation)
    {
        SimulationParams params = $simulation.getParams();
        double pctDelA = params.getDouble("mutation.rate.deleterious.apop");
        double pctDelD = params.getDouble("mutation.rate.deleterious.div");
        double pctBenA = params.getDouble("mutation.rate.beneficial.apop");
        double pctBenD = params.getDouble("mutation.rate.beneficial.div");
        double pctMut = params.getDouble("mutation.rate.mutator");
        
        double d = Simulation.RANDOM.randomDouble();
        if (d < pctDelA)
        {
            return DeleteriousApopMutation.create($simulation);
        }

        // This could be made more efficient by storing these sums, since they 
        // don't change
        if (d < pctDelA + pctDelD) 
        {
            return DeleteriousDivMutation.create($simulation);
        }
        
        if (d < pctDelA + pctDelD + pctBenA)
        {
            return BeneficialApopMutation.create($simulation);
        }
        
        if (d < pctDelA + pctDelD + pctBenA + pctBenD)
        {
            return BeneficialDivMutation.create($simulation);
        }
        
        if (d < pctDelA + pctDelD + pctBenA + pctBenD + pctMut)
        {
            return MutatorMutation.create($simulation);
        }
        
        return null;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** string tag of this mutation object for debug/output text */
    String _label = "";

    
    /**
     * Constructor.
     *
     */
    Mutation(Simulation $simulation)
    {
        super($simulation);
    }
    
    
    //
    // Label get/set
    //
    public void setLabel(String $label)
    {
        _label = $label;
    }
    
    public String getLabel()
    {
        return _label;
    }
    
    
    //
    // Default effect for all mutations -- no effect.
    //
    public double apoptosisEffect(double $rate) {return $rate;}
    public double divisionEffect(double $rate) {return $rate;}
    public double mutationEffect(double $rate) {return $rate;}
    
    
    /**
     * Return an XML representation of this mutation object.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<mutation>");

        pw.println("<type>");
        pw.println(getClass().getName());
        pw.println("</type>");

        pw.println("<label>");
        pw.println(getLabel());
        pw.println("</label>");

        pw.println("<effects>");
        pw.println("<apoptosis>");
        pw.println(apoptosisEffect(1.0));
        pw.println("</apoptosis>");
        pw.println("<division>");
        pw.println(divisionEffect(1.0));
        pw.println("</division>");
        pw.println("<mutation>");
        pw.println(mutationEffect(1.0));
        pw.println("</mutation>");
        pw.println("</effects>");
        
        pw.println("</mutation>");
        
        return sw.toString();
    }
}

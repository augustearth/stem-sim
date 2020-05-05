package stemsim.object;


import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.*;
import java.text.DecimalFormat;

import stemsim.simulation.*;


/**
 * SummaryMutation is a mutation that summarizes a group of other
 * mutations by storing their effects in a multiplicative fashion.
 *
 * For example, two beneficial division mutations may affect a cell's division
 * rate by 1.1 * 1.1.  If these two mutatoins were summarized by a 
 * SummaryMutation object, the value 1.1 * 1.1 would be stored in the 
 * _divMultiplier field.
 *
 */
public class SummaryMutation extends Mutation implements Cloneable
{
    /** the number of mutations that this object is summarizing */
    int _count;
    
    /** cumulative apoptosis multiplier of the summarized mutations */
    double _apopMultiplier = 1.0;

    /** cumulative division multiplier of the summarized mutations */
    double _divMultiplier = 1.0;
    
    /** cumulative mutation multiplier of the summarized mutations */
    double _mutMultiplier = 1.0;

    
    /**
     * Default constructor.
     *
     */
    public SummaryMutation(Simulation $simulation)
    {
        super($simulation);
    }

    
    /**
     * Convenience constructor to set the label of this mutation.
     *
     */
    public SummaryMutation(Simulation $simulation, String $label)
    {
        super($simulation);
        setLabel($label);
    }
    
    
    /**
     * Getter for count attribute.
     *
     */
    public int getCount()
    {
        return _count;
    }
    
    
    /**
     * Summarize the given mutation.
     *
     */
    public void summarize(Mutation $mutation)
    {
        _count++;
        
	    // Every mutation object has these 3 fields, only one of which is != 1.
	    // Thus, the Deleterious apoptosis mutation would return 1.1 for 
        // apoptosisEffect but 1 for the others.
        _apopMultiplier *= $mutation.apoptosisEffect(1.0);
        _divMultiplier *= $mutation.divisionEffect(1.0);
        _mutMultiplier *= $mutation.mutationEffect(1.0);
    }

    
    /**
     * StemCellAffector interface.
     *
     */
    public double apoptosisEffect(double $rate)
    {
        return $rate * _apopMultiplier;
    }
    
    public double divisionEffect(double $rate)
    {
        return $rate * _divMultiplier;
    }
    
    public double mutationEffect(double $rate)
    {
        return $rate * _mutMultiplier;
    }
    
    
    /**
     * Return a copy of this object.  Shallow copy is OK.
     *
     */
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
    
    
    /**
     * Return an XML representation of this SummaryMutation.
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
        
        pw.println("<count>");
        pw.println(_count);
        pw.println("</count>");

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

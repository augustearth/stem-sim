package stemsim.simulation;


import java.io.*;
import java.util.*;
import stemsim.event.*;
import stemsim.object.*;


/**
 * A single Crypt simulation.
 *
 */
public class CryptSimulation extends Simulation
{
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    Crypt _crypt;
    
    
    /**
     * Initialize by creating a crypt with approopriate number of stem cells.
     *
     */
    public void init(SimulationParams $params)
    {
        _crypt = initCrypt($params.getInt("crypt.numcells.mean"));
    }

    
    /**
     * Debug state is the crypt's state.
     *
     */
    public void debugPrintState()
    {
        System.out.println(_crypt);
    }
    
    
    /**
     * Can continue as long as the crypt is alive.
     *
     */
    public boolean canContinue()
    {
        return _crypt.isAlive();
    }
    
    
    /**
     * Return the crypt of this simulation.
     *
     */
    public Crypt getCrypt()
    {
        return _crypt;
    }
    
    
    /**
     * Return an XML representation of this simulation.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<cryptsim>");
        pw.println();
        
        pw.println("<lastevent>");
        pw.println(getLastEvent().toXML());
        pw.println("</lastevent>");
        pw.println();
        
        pw.println(getCrypt().toXML());
        pw.println();

        pw.println("</cryptsim>");
        pw.println();
        
        return sw.toString();
    }
    
    
    /**
     * Create and initialize a new crypt.
     *
     */
    Crypt initCrypt(int $numStemCells)
    {
        Crypt crypt = new Crypt(this, 0);
        for (int i=0; i<$numStemCells; i++)
        {
            StemCell cell = initStemCell(crypt);
        }
        return crypt;
    }

    
    /**
     * Create and initialize a stem cell inside a crypt.
     *
     */
    StemCell initStemCell(Crypt $crypt)
    {
        StemCell cell = StemCell.create($crypt);

        ApoptosisEvent.generate(0.0, cell);
        SymmetricDivisionEvent.generate(0.0, cell);
        
        return cell;
    }
}

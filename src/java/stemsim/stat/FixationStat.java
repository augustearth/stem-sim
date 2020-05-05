package stemsim.stat;


import java.io.*;
import java.util.*;

import stemsim.event.*;
import stemsim.object.*;
import stemsim.simulation.*;


/**
 * FixationStat monitors crypt births and deaths and records the times of each to
 * calculate the life-span of crypts in the simulation.
 *
 */
public class FixationStat extends SimEventListenerAdapter
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Return true if the mutator mutation is fixed in this crypt.
     *
     */
    boolean isMutator(Crypt $crypt)
    {
        if ($crypt.getStemCells().size() < 1) return false;
        
        for (Cell c : $crypt.getStemCells())
        {
            if (!c.isMutator()) return false;
        }
        
        return true;
    }
        
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Set of crpts that are currently fixed */
    Set<Integer> _fixedCrypts = null;
    
    // file stuff
    File _outfile = null;
    PrintWriter _out = null;

    
    /**
     * Conditionally record fixation event.
     *
     */
    void fix(Crypt $crypt, double $time)
    {
        if (_fixedCrypts.contains($crypt.getId())) return;
        
        _fixedCrypts.add($crypt.getId());
        print($crypt, $time);
    }
    
    
    /**
     * Remove a crypt from our list of fixed crypts.
     *
     */
    void unfix(Crypt $crypt)
    {
        _fixedCrypts.remove($crypt.getId());
    }
    
	
    /**
     * Start of simulation
     *
     */
    public void start(SimulationStartEvent $event) 
    {
        _fixedCrypts = new HashSet<Integer>();
        
        Simulation sim = $event.getSubject();
        SimulationParams params = sim.getParams();
        
        FileWriter fw = null;
        try
        {
            _outfile = new File(sim.getOutputDirectory(), "fixstat.txt");

            if (_outfile.exists())
            {
                fw = new FileWriter(_outfile, true);
                _out = new PrintWriter(fw);
            }
            else
            {
                fw = new FileWriter(_outfile);
                _out = new PrintWriter(fw);
                
                // header
                _out.print("sim");
                _out.print("\t");
                _out.print("crypt");
                _out.print("\t");
                _out.print("time");
                _out.println();
            }
            
            _out.flush();
        }
        catch (IOException e)
        {
            System.err.println("Could not write " + _outfile);
            e.printStackTrace();
        }
    }
    
    
    /**
     * In the event of crypt bifurcation, check if the daughter crypt is being
     * filled with all mutators (fixed).
     *
     */
    public void bifurcation(CryptBifurcationEvent $event)
    {
        Crypt daughter = $event.getDaughter();
        if (daughter != null)
        {
            fix(daughter, $event.getTime());
        }
    }
    
    
    /**
     * In the event of a stem cell apoptosis event, check for crypt death and
     * for crypt fixation.
     *
     */
    public void apoptosis(ApoptosisEvent $event)
    {
        Cell cell = $event.getSubject();
        Crypt crypt = $event.getCrypt();
        
        if (crypt.getStemCells().size() == 0)
        {
            unfix(crypt);
            return;
        }

        if (isMutator(crypt)) fix(crypt, $event.getTime());
    }
    
    
    /**
     * At the end of the simulation, write the output file.
     *
     */
    public void end(SimulationEndEvent $event)
    {
        try
        {
            _out.flush();
            _out.close();
        }
        catch (Exception e)
        {
            System.err.println("Could not write " + _outfile);
            e.printStackTrace();
        }
    }
    
    
    void print(Crypt $c, double $time)
    {
        Simulation sim = $c.getSimulation();

        _out.print(sim.getLabel());
        _out.print("\t");
        _out.print($c.getId());
        _out.print("\t");
        _out.print($time);
        
        _out.println();
    }
    
    
    public boolean equals(Object $obj)
    {
        if ($obj == null) return false;
        
        return ($obj instanceof FixationStat); 
    }
    
    public int hashCode()
    {
        return 0;
    }
    
}
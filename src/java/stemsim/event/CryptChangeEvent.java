package stemsim.event;


import java.io.*;
import java.util.*;
import java.util.PriorityQueue;

import lib.Probability;

import stemsim.object.*;
import stemsim.simulation.*;


/**
 * Represents a change in a crypt -- used to recreate stem cell events with
 * updated crypt effects whenever the number of stem cells in a crypt
 * changes.
 *
 */
public class CryptChangeEvent
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Perform the events of a CryptChangeEvent.  Convenience method so a 
     * CryptChangeEvent can be executed without creating a CryptChangeEvent
     * object.
     *
     */
    static public void run(Crypt $subject, double $time)
    {
        Simulation sim = $subject.getSimulation();
        SimulationParams params = sim.getParams();
        
        // if we have cancer, stop the simulation
        if ($subject.isCancerous())
        {
            // DEBUG
            System.err.println("OUT OF CONTROL: " + $subject);
            // DEBUG
            
            SimulationEndEvent se = SimulationEndEvent.create(sim);
            se.setTime($time);
            sim.getEventQueue().offer(se);
            return;
        }
        
        // re-generate the apoptosis/division events of each stem cell
        for (StemCell cell : $subject.getStemCells())
        {
            ApoptosisEvent.generateFeedback($time, cell);
            
            // Checks to see if we are in the middle of dividing. If so, don't 
            // reschedule
            if (!impending(sim, cell.getDivisionEvent(), $time)) 
            {
                SymmetricDivisionEvent.generate($time, cell);
            }
        }
        
        // if we're above bifurcation threshold and there is a dead neighbor...
        double mean = params.getDouble("crypt.numcells.mean");
        double ratio = params.getDouble("crypt.bifurcation.threshold.ratio");
        long thresh = Math.round(mean * ratio); // could store this value
        
        boolean canBifurcate = $subject.getStemCells().size() >= thresh
            && $subject.deadNeighbors().size() > 0;

        if (canBifurcate)
        {
            CryptBifurcationEvent be = new CryptBifurcationEvent($subject);
            be.setTime($time);
            sim.getEventQueue().offer(be);
        }
    }
    
    
    static boolean impending(Simulation $sim, 
                             SimulationEvent $event, 
                             double $time)
    {
        if ($event == null) return false;
        
        if (!($event instanceof SymmetricDivisionEvent
              || $event instanceof AsymmetricDivisionEvent))
        {
            throw new IllegalArgumentException(
                        "wrong event type: " + $event.getClass().getName());
        }
        
        SimulationParams params = $sim.getParams();
        double floor = params.getDouble("event.celldivision.floor");
        
        return ($event.getTime() - $time) < floor;
    }
    

    // DEBUG
    /*
    // run()
    Integer msi = milestones.get($subject);
    if (msi == null) 
    {
        milestone($subject);
        return;
    }
    
    int ms = msi.intValue();
    int numcells = $subject.getStemCells().size();
    
    if (numcells > ms) milestone($subject);
    
    
    static Map<Crypt,Integer> milestones = new HashMap<Crypt,Integer>();
    static PrintWriter mout = null;
    
    static void milestone(Crypt $crypt) 
    {
        Simulation sim = $crypt.getSimulation();

        if (mout == null)
        {
            try
            {
                File outdir = sim.getOutputDirectory();
                String fname = "milestones.txt"; 
                File moutfile = new File(outdir, fname);
                FileWriter fw = new FileWriter(moutfile);
                mout = new PrintWriter(fw);
                
                mout.print("crypt\t");
                mout.print("num cells\t");
                mout.print("time to apop\t");
                mout.print("time to feedback apop\t");
                mout.print("time to div\t");
                mout.print("doomed cells");
                
                mout.println();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        double avgApop = averageTimeToApop($crypt);
        double avgFeedbackApop = averageTimeToFeedbackApop($crypt);
        double avgDiv = averageTimeToDiv($crypt);
        int doomed = doomedCells($crypt);
        
        int numcells = $crypt.getStemCells().size();
        milestones.put($crypt, numcells);
        
        try
        {
            mout.print($crypt.getId());
            mout.print("\t");
            mout.print(numcells);
            mout.print("\t");
            mout.print(avgApop);
            mout.print("\t");
            mout.print(avgFeedbackApop);
            mout.print("\t");
            mout.print(avgDiv);
            mout.print("\t");
            mout.print(doomed);
            mout.println();

            mout.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    static double averageTimeToApop(Crypt $crypt)
    {
        double total = 0.0;

        for (StemCell cell : $crypt.getStemCells())
        {
            SimulationEvent se = cell.getApoptosisEvent();
            double timetoevent = se.getTime() - se.getClocked();
            total += timetoevent;
        }
        
        int numcells = $crypt.getStemCells().size();
        return total/numcells;
    }

    static double averageTimeToFeedbackApop(Crypt $crypt)
    {
        double total = 0.0;
        
        for (StemCell cell : $crypt.getStemCells())
        {
            SimulationEvent se = cell.getFeedbackApoptosisEvent();
            double timetoevent = se.getTime() - se.getClocked();
            total += timetoevent;
        }
        
        int numcells = $crypt.getStemCells().size();
        return total/numcells;
    }
    
    static double averageTimeToDiv(Crypt $crypt)
    {
        double total = 0.0;
        
        for (StemCell cell : $crypt.getStemCells())
        {
            SimulationEvent se = cell.getDivisionEvent();
            double timetoevent = se.getTime() - se.getClocked();
            total += timetoevent;
        }
        
        int numcells = $crypt.getStemCells().size();
        return total/numcells;
    }
    
    static int doomedCells(Crypt $crypt)
    {
        int total = 0;

        for (StemCell cell : $crypt.getStemCells())
        {
            if (cell.isDoomed()) total++;
        }
        
        return total;
    }
    */
    // DEBUG
    
}

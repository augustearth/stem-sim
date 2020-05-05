package stemsim.stat;


import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import stemsim.event.*;
import stemsim.object.*;
import stemsim.simulation.*;
import stemsim.util.*;


/**
 * TissuePopStat monitors crypts and records their stem cell population size as a
 * function of time.
 *
 */
public class TissuePopStat extends SimEventListenerAdapter
{
    static DecimalFormat dform = new DecimalFormat("#.##");
    static DecimalFormat lform = new DecimalFormat("#.####");
    
    
    /**
     * Result file processor.
     *
     */
    static public void main(String[] $args) throws Exception
    {
        File f = new File($args[0]);
        if (!f.exists())
        {
            throw new IllegalArgumentException($args[0] + " does not exist");
        }
        
        // get the data directory from command line
        String resDirName = $args[0];
        File rdir = new File(resDirName);
        
        // all the data we gather
        int totalStemCells = 0;
        int totalLivingCrypts = 0;
        int totalCrypts = 0;

        double totalCellsPerCrypt = 0;
        double totalCellsPerLivingCrypt = 0;
        
        int count = 0;
        
        // recursively find all tissuepopstat.txt files in data directory
        List<File> rfiles = new LinkedList<File>();
        FilenameFilter flt = new FileUtil.ExactMatchFilter("tissuepopstat.txt");
        FileUtil.recursiveGetFiles(rdir, flt, rfiles);
        
        List<String> skipped = new LinkedList<String>();

        // loop through tissuepopstat files
        for (File rfile : rfiles)
        {
            BufferedReader in = new BufferedReader(new FileReader(rfile));
            String line = null;
            while ((line = in.readLine()) != null)
            {
                System.err.println(line);
                
                String[] tokens = line.split("\\s");
                
                // skip the header line
                if (tokens.length != 5)
                {
                    skipped.add(line);
                    continue;
                }
                
                /*
                 for (String t : tokens)
                 {
                 System.err.println(t);
                 }
                 */
                
                int crypts = Integer.parseInt(tokens[2]);
                int livingCrypts = Integer.parseInt(tokens[3]);
                int cells = Integer.parseInt(tokens[4]);
                
                double cellsPerCrypt = (double)cells / (double)crypts;
                double cellsPerLivingCrypt = (double)cells / (double)livingCrypts;
                
                totalCrypts += crypts;
                totalLivingCrypts += livingCrypts;
                totalStemCells +=  cells;
                
                totalCellsPerCrypt += cellsPerCrypt;
                totalCellsPerLivingCrypt += cellsPerLivingCrypt;
                
                count++;
            }
            in.close();
        }
       
        if (skipped.size() > 0)
        {
            System.err.println("skipped lines");
            for (String s : skipped)
            {
                System.err.println(s);
            }
        }
        
        double avgCells = totalStemCells / (double)count;
        double avgAllCrypts = totalCellsPerCrypt / (double)count;
        double avgLivingCrypts = totalCellsPerLivingCrypt / (double)count;
        
        double avgNumLivingCrypts = totalLivingCrypts / (double)count;
        
        System.out.println("avg num living crypts = " + avgNumLivingCrypts);
        System.out.println("avg cells = " + avgCells);
        System.out.println("avg cells per crypt = " + avgAllCrypts);
        System.out.println("avg cells per living crypt = " + avgLivingCrypts);
        
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        String fname = null;
        
        fname = rdir.getName() + "_tpop.txt";
        outfile = new File(fname);
        fw = new FileWriter(outfile);
        out = new PrintWriter(fw);
        
        out.print("avg num living crypts");
        out.print("\t");
        out.print("avg cells");
        out.print("\t");
        out.print("avg cells per crypt");
        out.print("\t");
        out.print("avg cells per living crypt");

        out.println();
        out.print(avgNumLivingCrypts);
        out.print("\t");
        out.print(avgCells);
        out.print("\t");
        out.print(avgAllCrypts);
        out.print("\t");
        out.print(avgLivingCrypts);
        out.println();
        
        // close out file
        out.flush();
        out.close();
    }
    
    /* list of pop measurements taken at regular intervals */
    List<PopMeasurement> _pops;
    
    // interval in days to take measurements
    int _interval = -1;
    
    // file stuff
    File _outfile = null;
    PrintWriter _out = null;
    
    
    /**
     * Start of new simulation
     *
     */ 
    public void start(SimulationStartEvent $event) 
    {
        _pops = new LinkedList<PopMeasurement>();
        
        Simulation sim = $event.getSubject();
        SimulationParams params = sim.getParams();
        
        _interval = params.getInt("tissuepopstat.interval");
        
        FileWriter fw = null;
        String fname = "tissuepopstat.txt";
        try
        {
            
            _outfile = new File(sim.getOutputDirectory(), fname);
            
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
                _out.print("time");
                _out.print("\t");
                _out.print("total crypts");
                _out.print("\t");
                _out.print("living crypts");
                _out.print("\t");
                _out.print("total stem cells");
                _out.println();
                
                _out.flush();
            }
        }
        catch (IOException e)
        {
            System.err.println("could not init " + fname);
            e.printStackTrace();
        }
    }
    
    
    public void division(SymmetricDivisionEvent $event)
    {
        StemCell subject = $event.getSubject();
        Crypt crypt = subject.getCrypt();
        
        Simulation sim = crypt.getSimulation();
        if (!(sim instanceof TissueSimulation)) return;
        
        TissueSimulation tsim = (TissueSimulation)sim;
        Tissue tissue = tsim.getTissue();
        
        takeMeasurement(tissue, $event.getTime());
    }

    
    void takeMeasurement(Tissue $tissue, double $time)
    {
        double lasttime = -_interval;

        if (_pops.size() > 0)
        {
            PopMeasurement last = _pops.get(_pops.size() - 1);
            lasttime = last.time;
        }
        
        if ($time - lasttime > _interval)
        {
            measure($tissue, $time);
        }
    }

    
    void measure(Tissue $tissue, double $time)
    {
        Simulation sim = $tissue.getSimulation();
        
        PopMeasurement pm = new PopMeasurement();
        pm.sim = sim.getLabel();
        pm.time = $time;
        
        for (Crypt c: $tissue.getCrypts())
        {
            pm.totalCrypts++;
            
            if (c.isAlive()) pm.livingCrypts++;
            
            pm.totalCells += c.getStemCells().size();
        }
        
        _pops.add(pm);
        
        _out.print(pm.sim);
        _out.print("\t");
        _out.print(dform.format(pm.time));
        _out.print("\t");
        _out.print(pm.totalCrypts);
        _out.print("\t");
        _out.print(pm.livingCrypts);
        _out.print("\t");
        _out.print(pm.totalCells);
        
        _out.println();
    }
    
    
    /**
     * At the end of the simulation, write the output file.
     *
     */
    public void end(SimulationEndEvent $event)
    {
        System.err.println("END");

        _out.flush();
        _out.close();
    }
    
    
    public boolean equals(Object $obj)
    {
        if ($obj == null) return false;
        
        return ($obj instanceof TissuePopStat); 
    }

    
    public int hashCode()
    {
        return 0;
    }
    
    
    class PopMeasurement
    {
        String sim;
        double time;
        int totalCrypts;
        int livingCrypts;
        int totalCells;
        
        public String toString()
        {
            return sim + " " + time + " " + totalCrypts + " "
            + livingCrypts + " " + totalCells;
        }
    }
}
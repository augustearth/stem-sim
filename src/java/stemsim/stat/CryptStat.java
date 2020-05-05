package stemsim.stat;


import java.io.*;
import java.util.*;
import java.util.regex.*;

import stemsim.event.*;
import stemsim.object.*;
import stemsim.simulation.*;
import stemsim.util.*;


/**
 * CryptStat monitors crypt births and deaths and records the times of each to
 * calculate the life-span of crypts in the simulation.
 *
 */
public class CryptStat extends SimEventListenerAdapter
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    public static void main(String[] $args) throws Exception
    {
        if ($args.length < 1)
        {
            System.err.println("Usage: java stemsim.stat.CryptStat"
                               + " <data directory>");
        }
        
        // get the data directory from command line
        String resDirName = $args[0];
        File rdir = new File(resDirName);
        
        // recursively find all .xml files in data directory
        List<File> rfiles = new LinkedList<File>();
        FilenameFilter flt = new FileUtil.ExactMatchFilter("cryptstat.txt");
        FileUtil.recursiveGetFiles(rdir, flt, rfiles);
        
        // to store our data
        List<LifeSummary> lifespans = new ArrayList<LifeSummary>();
        
        // loop through cryptstat files
        for (File rfile : rfiles)
        {
            System.err.println();
            System.err.println(rfile.getCanonicalPath());
            
            // get xml file
            File dir = rfile.getParentFile();
            File run1xml = new File(dir, "run01_01.xml");
            if (!run1xml.exists())
            {
                throw new 
                IllegalArgumentException("could not find run01_01.xml");
            }
            System.err.println(run1xml.getCanonicalPath());

            // gather/package data 
            LifeSummary ls = new LifeSummary();
            ls.delprop = delProp(run1xml);
            ls.cells = cells(run1xml);
            ls.maxDuration = maxDuration(run1xml);
            
            // TODO: figure out why the hell i'm doing this.  it seems like
            // i should be getting crypt stats from all xml files
            loadCryptStats(ls, dir);
            
            ls.lifespans = lifespans(rfile);
            
            // debug
            System.err.print(ls.numCrypts());
            System.err.print(" ");
            System.err.print(ls.numDeadCrypts());
            System.err.print(" ");
            System.err.print(ls.numDeaths());
            System.err.print(" ");
            System.err.print(ls.numBirths());
            System.err.println();
            
            lifespans.add(ls);
        }
        
        // write graph data
        writeGraphData(rdir, lifespans);
        
        // write lifespans file
        writeLifespanDist(rdir, lifespans, 10);

        // write lifespans avg over all del props file
        writeLifespanDistAllDelProps(rdir, lifespans, 10);
    }
    
    
    static void loadCryptStats(LifeSummary $sum, File $dir)
    throws IOException, FileNotFoundException
    {
        for (int i=1; i<=50; i++)
        {
            String fnum = "" + i;
            if (i < 10) fnum = "0" + fnum;
            
            String fname = "run01_" + fnum + ".xml";
            File runxml = new File($dir, fname);
            if (!runxml.exists())
            {
                throw new 
                IllegalArgumentException("could not find "
                + runxml.getCanonicalPath());
            }
            
            $sum.addCryptStats(cryptStats(runxml));
            
            // we need to do better than this.  need to record the sim number
            // so we could link with the sim numbers in the cryptstat.txt file
            // too much work and probably not worth it
            //$sum.duration = duration(runxml);
        }
    }
    

    
    static void writeGraphData(File $dir,
                               List<LifeSummary> $sums)
        throws IOException
    {
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        String fname = null;
        
        fname = $dir.getName() + "_lifegraph.txt";
        outfile = new File(fname);
        fw = new FileWriter(outfile);
        out = new PrintWriter(fw);
        
        SortedSet<Integer> ncs = numCells($sums);
        SortedSet<Integer> dps = delProps($sums);
        
        // headers
        out.print("data");
        out.print("\t");
        out.print("cells");
        for (int d : dps)
        {
            out.print("\t");
            out.print(d);
        }
        out.println();
        
        // avg crypt lifespan
        for (int nc : ncs)
        {
            out.print("avg crypt lifespan");
            out.print("\t");
            out.print(nc);

            for (int d : dps)
            {
                LifeSummary ls = get($sums, nc, d);
                
                out.print("\t");
                out.print(Calc.mean(ls.lifespans));
            }

            out.println();
        }
        out.println();
        
        // avg crypt lifespan standard err
        for (int nc : ncs)
        {
            out.print("avg crypt lifespan std err");
            out.print("\t");
            out.print(nc);
            
            for (int d : dps)
            {
                LifeSummary ls = get($sums, nc, d);
                
                out.print("\t");
                out.print(Calc.stErr(ls.lifespans));
            }
            
            out.println();
        }
        out.println();

        // num births
        for (int nc : ncs)
        {
            out.print("num births");
            out.print("\t");
            out.print(nc);
            
            for (int d : dps)
            {
                LifeSummary ls = get($sums, nc, d);
                
                out.print("\t");
                out.print(ls.numBirths());
            }
            
            out.println();
        }
        out.println();

        // close out file
        out.flush();
        out.close();
    }
    

    
    static void writeLifespanDist(File $dir, 
                                  List<LifeSummary> $sums,
                                  int $numBins)
    throws IOException
    {
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        String fname = null;
        
        fname = $dir.getName() + "_lifedist.txt";
        outfile = new File(fname);
        fw = new FileWriter(outfile);
        out = new PrintWriter(fw);
        
        SortedSet<Integer> ncs = numCells($sums);
        SortedSet<Integer> dps = delProps($sums);
        
        int numbins = $numBins;
        
        // headers
        out.print("data");
        out.print("\t");
        out.print("cells");
        out.print("\t");
        out.print("del prop");
        for (int i=0; i<numbins; i++)
        {
            out.print("\t");
            out.print(i);
        }
        out.println();
        
        for (int nc : ncs)
        {
            for (int d : dps)
            {
                LifeSummary ls = get($sums, nc, d);
                
                // calculate our distribution
                Distribution dist = new Distribution();
                dist.init(ls.lifespans, numbins);

                // bins
                out.print("bins");
                out.print("\t");
                out.print(nc);
                out.print("\t");
                out.print(d);
                for (int i=0; i<numbins; i++)
                {
                    out.print("\t");
                    out.print(dist.getBins()[i]);
                }
                out.println();

                // vals
                out.print("vals");
                out.print("\t");
                out.print(nc);
                out.print("\t");
                out.print(d);
                for (int i=0; i<numbins; i++)
                {
                    out.print("\t");
                    out.print(dist.getVals()[i]);
                }
                out.println();
            }
        }
        
        // close out file
        out.flush();
        out.close();
    }
    
    
    static void writeLifespanDistAllDelProps(File $dir, 
                                             List<LifeSummary> $sums,
                                             int $numBins)
    throws IOException
    {
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        String fname = null;
        
        fname = $dir.getName() + "_lifedistavg.txt";
        outfile = new File(fname);
        fw = new FileWriter(outfile);
        out = new PrintWriter(fw);
        
        SortedSet<Integer> ncs = numCells($sums);
        SortedSet<Integer> dps = delProps($sums);
        
        int numbins = $numBins;
        
        // headers
        out.print("data");
        out.print("\t");
        out.print("cells");
        for (int i=0; i<numbins; i++)
        {
            out.print("\t");
            out.print(i);
        }
        out.println();
        
        for (int nc : ncs)
        {
            List<Double> lifespans = new ArrayList<Double>();

            for (int d : dps)
            {
                LifeSummary ls = get($sums, nc, d);
                lifespans.addAll(ls.lifespans);
            }
                
            // calculate our distribution
            Distribution dist = new Distribution();
            dist.init(lifespans, numbins);
            
            // bins
            out.print("bins");
            out.print("\t");
            out.print(nc);
            for (int i=0; i<numbins; i++)
            {
                out.print("\t");
                out.print(dist.getBins()[i]);
            }
            out.println();
            
            // vals
            out.print("vals");
            out.print("\t");
            out.print(nc);
            for (int i=0; i<numbins; i++)
            {
                out.print("\t");
                out.print(dist.getVals()[i]);
            }
            out.println();
        }
        
        // close out file
        out.flush();
        out.close();
    }
    
    
    static class Distribution
    {
        double min;
        double max;
        double chunk;
        double numbins;
        
        double[] bins;
        int[] vals;

        public Distribution()
        {
            // no-op
        }
        
        void init(List<Double> $vals, int $numBins)
        {
            // set up our bins and vals variables
            bins = new double[$numBins];
            vals = new int[$numBins];
            
            // no values, return a zeroed out object
            if ($vals == null || $vals.size() < 1) return;
            
            // find the minimum and mazimum values
            max = $vals.get(0);
            min = $vals.get(0);
            for (double d : $vals)
            {
                if (d < min) min = d;
                
                if (d > max) max = d;
            }
            
            // calculate a cunk size
            chunk = (max - min) / (double)$numBins;
            
            // if chunk size is zero, either we've got only one value or more
            // than on value that are super close to each other.  in this case,
            // we're not going to have much of a distribution, so just do 
            // something so that code doesn't crash
            if (chunk == 0) chunk = Math.abs(0.1d * min);
            
            // track which vals get used
            boolean[] used = new boolean[$vals.size()];
            for (int i=0; i<used.length; i++) used[i] = false;

            // x and y are the lower and upper bounds of the interval or bin for
            // which we are currently counting objects
            double x = 0;
            double y = 0;

            // record the bins
            x = min;
            for (int i=0; i < $numBins; i++)
            {
                // record the min value of the bin
                bins[i] = x;
                
                // reset for next iteration
                x = x + chunk;
            }
                
            // for the actual calculations, we need to set the min for the first
            // bin below the value of min so it gets caught.  similar for the
            // max for last iteration
            x = min - (0.1d * (max - min));
            y = min + chunk;
            
            for (int i=0; i < $numBins; i++)
            {
                // count the number of vals in the bin
                int c = 0;
                for (int j=0; j<$vals.size(); j++)
                {
                    double d = $vals.get(j);
                    if (d > x && d <= y) 
                    {
                        c++;
                        used[j] = true;
                    }
                }
                
                vals[i] = c;
                
                // reset for next iteration
                x = y;
                y = x + chunk;
                
                // special case the last bin.  
                // make sure it's bigger than max val
                if (i == $numBins - 2)
                {
                    y = max + (0.1d * (max - min));
                }
            }
            
            // debug
            int sum = 0;
            for (int c : vals)
            {
                sum += c;
            }
            System.err.println($vals.size() + " " + sum);
            
            for (int i=0; i<used.length; i++) {
                if (!used[i])
                {
                    System.err.print($vals.get(i));
                    System.err.print(" ");
                }
            }
            System.err.println();
        }
        
        public int numBins()
        {
            return bins.length;
        }
        
        public int[] getVals()
        {
            return vals;
        }
        
        public double[] getBins()
        {
            return bins;
        }
    }
    
    
    static class LifeSummary
    {
        double maxDuration;
        int delprop;
        int cells;
        int crypts;
        int numDeadCrypts;
        List<Double> lifespans;
        List<String> cryptStats;

        // was going to capture the duration of the simulation, but this object
        // summarizes over all sims.  if we want to use the actual simulation
        // duration properly, we would have to link it to the lifespans by sim
        // number and in our calculations make sure that we were using the given
        // duration the right number of times (for the right number of crypts)
        // not even sure it makes sense to do so, so abondoning this effort
        // double duration;

        public void addCryptStats(List<String> $cs)
        {
            if (cryptStats == null)
                cryptStats = new ArrayList<String>();
            
            cryptStats.addAll($cs);
        }
        
        public int numCrypts()
        {
            return cryptStats.size();
        }
        
        public int numDeadCrypts()
        {
            int nd = 0;
            for (String s : cryptStats)
            {
                if (s.equals("false")) nd++;
            }
            return nd;
        }
        
        public int numNeverDied()
        {
            return numCrypts() - numDeaths();
        }
        
        public int numDeaths()
        {
            return lifespans.size();
        }
        
        public int numBirths()
        {
            return numDeaths() - numDeadCrypts();
        }
        
        public List<Double> lifespans()
        {
            return null;
        }
        
        public String toString()
        {
            return "" + cells + " : "+ delprop + " : " + lifespans;
        }
    }
    
    
    static LifeSummary get(List<LifeSummary> $sums, 
                           int $numcells,
                           int $delprop)
    {
        for (LifeSummary ls : $sums)
        {
            if (ls.delprop == $delprop
                && ls.cells == $numcells) 
                return ls;
        }
        
        return null;
    }
    
    
    static public SortedSet<Integer> delProps(List<LifeSummary> $sums)
    {
        SortedSet<Integer> ss = new TreeSet<Integer>();
        for (LifeSummary ls : $sums)
        {
            ss.add(ls.delprop);
        }
        return ss;
    }
    
    static public SortedSet<Integer> numCells(List<LifeSummary> $sums)
    {
        SortedSet<Integer> ss = new TreeSet<Integer>();
        for (LifeSummary ls : $sums)
        {
            ss.add(ls.cells);
        }
        return ss;
    }
    
    
    static List<Double> lifespans(File $file)
        throws IOException, FileNotFoundException
    {
        List<Double> lives = new ArrayList<Double>();
        
        BufferedReader in = new BufferedReader(new FileReader($file));
        String line = null;
        
        // skip first line
        line = in.readLine();
        
        while ((line = in.readLine()) != null)
        {
            // System.err.println(line);
            
            String[] tokens = line.split("\\s");
            double life = Double.parseDouble(tokens[2]);
            lives.add(life);
        }
        
        return lives;
    }
    
    
    static List<String> cryptStats(File $file)
        throws IOException, FileNotFoundException
    {
        Pattern pat = Pattern.compile
        (
         "<crypt>\\s+<id>\\d+</id>\\s+<alive>(\\S+)</alive>"
         );
        
        BufferedReader in = null;
        String line = null;
        Matcher m = null;
        String pval = null;
        
        in = new BufferedReader(new FileReader($file));
        StringBuffer buf = new StringBuffer();
        while ((line = in.readLine()) != null)
        {
            buf.append(line);
            buf.append(" ");
        }
        
        List<String> stats = new ArrayList<String>();
        m = pat.matcher(buf);
        while (m.find())
        {
            stats.add(m.group(1));
        }
        
        return stats;
    }

    
    
    static int delProp(File $file) 
        throws IOException, FileNotFoundException
    {
        String dap = getProp($file, "mutation.rate.deleterious.apop");
        float da = Float.parseFloat(dap);

        String ddp = getProp($file, "mutation.rate.deleterious.div");
        float dd = Float.parseFloat(ddp);

        return Math.round((da + dd) * 100f);
    }
    
    
    static int cells(File $file)
        throws IOException, FileNotFoundException
    {
        String val = getProp($file, "crypt.numcells.mean");
        return Integer.parseInt(val);
    }

    static double maxDuration(File $file)
    throws IOException, FileNotFoundException
    {
        String val = getProp($file, "simulation.duration");
        return Double.parseDouble(val);
    }
    
    static String getProp(File $file, String $prop)
        throws IOException, FileNotFoundException
    {
        Pattern pat = Pattern.compile
        (
         "<entry key=\""
         + $prop
         + "\">(\\S+)</entry>"
        );
        
        BufferedReader in = null;
        String line = null;
        Matcher m = null;
        String pval = null;
        
        in = new BufferedReader(new FileReader($file));
        while ((line = in.readLine()) != null)
        {            
            m = pat.matcher(line);
            if (m.find())
            {
                return m.group(1);
            }
        }
        
        return null;
    }

    
    static double duration(File $file)
    throws IOException, FileNotFoundException
    {
        Pattern pat = Pattern.compile
        (
         "<lastevent>"
         + ".*"
         + "<time>"
         + "\\s*(\\S+)\\s*"
         + "</time>"
         + ".*"
         + "</lastevent>"
         );
        
        BufferedReader in = null;
        String line = null;
        Matcher m = null;
        String val = null;
        int i=0;
        StringBuffer sbuff = new StringBuffer();
        boolean eof = false;
        
        in = new BufferedReader(new FileReader($file));
        
        // we know the last event is early on in the file, so we can just
        // read the first 1000 lines and that should do it
        while (!eof && val == null)
        {
            // read 1000 lines
            while (i++ < 1000)
            {            
                line = in.readLine();
                if (line == null)
                {
                    eof = true;
                    break;
                }
                
                sbuff.append(line);
            }
            
            // try to match
            m = pat.matcher(sbuff);
            if (m.find())
            {
                val = m.group(1);
            }
            
            // reset line counter
            i = 0;
        }
        
        if (val == null) 
            throw new IllegalArgumentException("could not get duration from " 
                                               + $file);
        
        return Double.parseDouble(val);
    }
    
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    /** A map of crypts to their birth time */
    Map<Crypt,Double> _cryptbirths = new HashMap<Crypt, Double>();
    
    // file stuff
    File _outfile = null;
    PrintWriter _out = null;

	
    /**
     * Start of simulation
     *
     */
    public void start(SimulationStartEvent $event) 
    {
        _cryptbirths = new HashMap<Crypt, Double>();
        Simulation sim = $event.getSubject();
        SimulationParams params = sim.getParams();
        
        FileWriter fw = null;
        try
        {
            _outfile = new File(sim.getOutputDirectory(), "cryptstat.txt");

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
                _out.print("lifespan");
                _out.println();
            }
            
            _out.flush();
        }
        catch (IOException e)
        {
            System.err.println("Could not write " + _outfile);
            e.printStackTrace();
        }
		// add the initial crypts
		// for (Crypt leftmost : 
    }
    
    
    /**
     * In the event of crypt bifurcation, record the birth time of the daughter
     * crypt.
     *
     */
    public void bifurcation(CryptBifurcationEvent $event)
    {
        Crypt daughter = $event.getDaughter();
        if (daughter != null)
        {
            _cryptbirths.put(daughter, $event.getTime());
        }
    }
    
    
    /**
     * In the event of a stem cell apoptosis event, check for crypt death, 
     * record lifespan if so.
     *
     */
    public void apoptosis(ApoptosisEvent $event)
    {
        Cell cell = $event.getSubject();
        Crypt crypt = $event.getCrypt();
        
        if (crypt.getStemCells().size() > 0)
        {
            return;
        }
        
        Double birth = _cryptbirths.get(crypt);
        if (birth != null)
        {
            _cryptbirths.remove(crypt);
        }
        
        if (birth == null)
        {
            birth = new Double(0.0);
        }
        
        double b = birth.doubleValue();
        double d = $event.getTime();
        double lifespan = d - b;
        print(crypt, lifespan);
    }
    
    
    /**
     * At the end of the simulation, write the output file.
     *
     */
    public void end(SimulationEndEvent $event)
    {
        Simulation sim = $event.getSubject();
        SimulationParams params = sim.getParams();
        
        double d = $event.getTime();
        
        try
        {
            for (Crypt c : _cryptbirths.keySet())
            {
                double b = _cryptbirths.get(c);
                double lifespan = d - b;
                print(c, lifespan);
            }
            
            _out.flush();
            _out.close();
        }
        catch (Exception e)
        {
            System.err.println("Could not write " + _outfile);
            e.printStackTrace();
        }

    }
    
    
    void print(Crypt $c, double $life)
    {
        Simulation sim = $c.getSimulation();

        _out.print(sim.getLabel());
        _out.print("\t");
        _out.print($c.getId());
        _out.print("\t");
        _out.print($life);
        
        _out.println();
    }
    
    
    public boolean equals(Object $obj)
    {
        if ($obj == null) return false;
        
        return ($obj instanceof CryptStat); 
    }
    
    public int hashCode()
    {
        return 0;
    }
    
    
    /**
     * Data structure to record a crypt lifespan.
     *
     */
    class Lifespan
    {
        public int _id;
        public double _span;
        
        public Lifespan(int $id, double $span)
        {
            _id = $id;
            _span = $span;
        }
    }
    
}
package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


/**
 * DistanceStat is an executable class that computes the cancer related 
 * statistics from XML final simulation state files.
 *
 */
public class DistanceStat extends GenericStat
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    static public void main(String[] $args) throws Exception
    {
        if ($args.length < 1)
        {
            System.err.println("Usage: java stemsim.statxml.DistanceStat"
                               + " <data directory>");
        }
        
        // get the data directory from command line
        String resDirName = $args[0];
        File rdir = new File(resDirName);
        
        DistanceStat stat = new DistanceStat(rdir);
        stat.compile();
        stat.writeOutputFiles();
    }
    
    
    public DistanceStat(File $dataDir)
    {
        super($dataDir);
    }
    
    
    public void writeOutputFiles() throws Exception
    {
        // shared variables
        SortedSet<Integer> ncs = RunStats.getNumCells(validStats());
        
        // write output files
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        String fname = null;
        try
        {
            RunStats firstRStats = validStats().get(0);

            // avg fitness split by fixed mutator for cluster graphing
            fname = filename("_distfit.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("data type");
            out.print("\t");
            out.print("cells per crypt");
            out.print("\t");
            out.print("del prop");
            out.print("\t");
            out.print("label");
            
            for (DistanceClass dc : distanceClasses(firstRStats))
            {
                out.print("\t");
                out.print(dc.getDistance());
            }
            out.println();
            
            for (int nc : ncs)
            {
                for (int d : new int[]{50, 70, 90})
                {
                    out.print("avg fitness distance");
                    out.print("\t");
                    out.print(nc);
                    out.print("\t");
                    out.print(d);
                    out.print("\t");
                    out.print(nc + "," + d);
                    
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    // map distance class -> fit dist
                    SortedMap<Integer,List<Double>> vals = distFits(rstats);
                    
                    for (int k : vals.keySet())
                    {
                        List<Double> l = vals.get(k);
                        out.print("\t");
                        out.print(Calc.mean(l));
                    }
                    out.println();
                }
            }
            
            out.flush();
            out.close();
            
            
            // mutator agreement
            fname = filename("_distmut.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("data type");
            out.print("\t");
            out.print("cells per crypt");
            out.print("\t");
            out.print("del prop");
            out.print("\t");
            out.print("label");

            for (DistanceClass dc : distanceClasses(firstRStats))
            {
                out.print("\t");
                out.print(dc.getDistance());
            }
            out.println();
            
            for (int nc : ncs)
            {
                for (int d : new int[]{50, 70, 90})
                {
                    out.print("avg mutator agreement");
                    out.print("\t");
                    out.print(nc);
                    out.print("\t");
                    out.print(d);
                    out.print("\t");
                    out.print(nc);
                    
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    // map distance class -> mutator agreement
                    SortedMap<Integer,List<Double>> vals = distMuts(rstats);
                    
                    for (int k : vals.keySet())
                    {
                        List<Double> l = vals.get(k);
                        out.print("\t");
                        out.print(Calc.mean(l));
                    }
                    out.println();
                }
            }
            
            out.flush();
            out.close();
            
        }
        catch (IOException e)
        {
            System.err.println("could not write: " + fname);
            e.printStackTrace();
        }
    }
    
    
    SortedMap<Integer,List<Double>> distFits(List<RunStats> $rstats)
    {
        SortedMap<Integer,List<Double>> vals = 
            new TreeMap<Integer,List<Double>>();
        
        for (RunStats rs : $rstats)
        {
            for (DistanceClass dc : distanceClasses(rs))
            {
                List<Double> l = vals.get(dc.getDistance());
                if (l == null)
                {
                    l = new ArrayList<Double>();
                    vals.put(dc.getDistance(), l);
                }
                l.add(dc.avgFitnessDistance());
            }
        }
        
        return vals;
    }

    
    SortedMap<Integer,List<Double>> distMuts(List<RunStats> $rstats)
    {
        SortedMap<Integer,List<Double>> vals = 
        new TreeMap<Integer,List<Double>>();
        
        for (RunStats rs : $rstats)
        {
            for (DistanceClass dc : distanceClasses(rs))
            {
                List<Double> l = vals.get(dc.getDistance());
                if (l == null)
                {
                    l = new ArrayList<Double>();
                    vals.put(dc.getDistance(), l);
                }
                l.add(dc.avgMutatorAgreement());
            }
        }
        
        return vals;
    }
    
    
    Collection<DistanceClass> distanceClasses(RunStats $rstats)
    {
        List<CryptStats> cs = $rstats.cryptStats;
        CryptStats.setHexPoints(cs);
        return distanceClasses(cs);
    }
    
    Collection<DistanceClass> distanceClasses(List<CryptStats> $crypts)
    {
        SortedMap<Integer, DistanceClass> classes = 
            new TreeMap<Integer, DistanceClass>();
        
        // compute all pairs
        Set<Pair> pairs = new HashSet<Pair>();
        for (CryptStats a : $crypts)
        {
            for (CryptStats b : $crypts)
            {
                // skip if a and b are the same
                if (a == b) continue;

                pairs.add(new Pair(a, b));
            }
        }
        
        for (Pair pair : pairs)
        {
            Iterator<CryptStats> iter = pair.iterator();
            CryptStats a = iter.next();
            CryptStats b = iter.next();
            
            int distance = a.hexDistance(b);
            
            DistanceClass dc = classes.get(distance);
            if (dc == null)
            {
                dc = new DistanceClass(distance);
                classes.put(distance, dc);
            }
            
            dc.add(pair);
        }
        
        return classes.values();
    }
    
    
    
    
    class DistanceClass
    {
        int _distance;
        Set<Pair> _pairs = new HashSet<Pair>();
        
        public DistanceClass(int $distance)
        {
            _distance = $distance;
        }
        
        void add(Pair $pair)
        {
            _pairs.add($pair);
        }
        
        Set<Pair> getPairs()
        {
            return _pairs;
        }
                                                      
        int getDistance()
        {
            return _distance;
        }
                                                      
        double avgFitnessDistance()
        {
            List<Double> fds = new ArrayList<Double>(_pairs.size());
            for (Pair pair : _pairs)
            {
                fds.add(pair.fitnessDistance());
            }
            return Calc.mean(fds);
        }

        double avgMutatorAgreement()
        {
            List<Double> fds = new ArrayList<Double>(_pairs.size());
            for (Pair pair : _pairs)
            {
                fds.add(pair.mutatorAgreement());
            }
            return Calc.mean(fds);
        }
        
        public String toString()
        {
            String nl = System.getProperty("line.separator");
            
            StringBuffer buf = new StringBuffer();
            buf.append("[");
            buf.append(_distance);
            buf.append("]");
            buf.append(nl);
            
            for (Pair pair : _pairs)
            {
                buf.append(pair);
                buf.append(nl);
            }
            
            buf.append(nl);
            
            return buf.toString();
        }
    }
    
    
    class Pair extends ArrayList<CryptStats>
    {
        public Pair()
        {
            super(2);
        }
        
        public Pair(CryptStats $a, CryptStats $b)
        {
            this();
            add($a);
            add($b);
        }
        
        CryptStats a()
        {
            return get(0);
        }
        
        CryptStats b()
        {
            return get(1);
        }

        double fitnessDistance()
        {
            return Math.abs(b().fitness() - a().fitness());
        }
        
        double mutatorAgreement()
        {
            CryptStats a = a();
            CryptStats b = b();
            
            if (a.fixedMutator && b.fixedMutator)
                return 1.0;
            else if (a.fixedMutator || b.fixedMutator)
                return 0.0;
            else 
                return 1.0;
        }
        
    }
    
}

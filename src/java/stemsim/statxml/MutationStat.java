package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


/**
 * MutatorStat is an executable class that computes the cancer related 
 * statistics from XML final simulation state files.
 *
 */
public class MutationStat extends GenericStat
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    static DecimalFormat tform = new DecimalFormat("#.##");
    static DecimalFormat dform = new DecimalFormat("#.#######");
    
    
    static public void main(String[] $args) throws Exception
    {
        if ($args.length < 1)
        {
            System.err.println("Usage: java stemsim.statxml.MutationStat"
                               + " <data directory>");
        }
        
        // get the data directory from command line
        String resDirName = $args[0];
        File rdir = new File(resDirName);
        
        MutationStat stat = new MutationStat(rdir);
        stat.compile();
        stat.writeOutputFiles();
    }
    
    
    public MutationStat(File $dataDir)
    {
        super($dataDir);
    }
    
    
    public void writeOutputFiles() throws Exception
    {
        // write output files
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        String fname = null;
        try
        {
            // pct mutator across dell props all runs
            fname = filename("_mstat.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("del prop");
            out.print("\t");
            out.print("mutator crypts");
            out.print("\t");
            out.print("mutator 50 crypts");
            out.print("\t");
            out.print("mutator 100 crypts");
            out.print("\t");
            out.print("mutator 100 crypts sterr");
            out.println();
            
            for (MutatorStat mstat : mutatorStats())
            {
                out.print(mstat.delProp);
                out.print("\t");
                out.print(mstat.pctMutatorCrypts());
                out.print("\t");
                out.print(mstat.pctMutatorCrypts50());
                out.print("\t");
                out.print(mstat.pctMutatorCrypts100());
                out.print("\t");
                out.print(mstat.pctMutatorCrypts100StErr());
                out.println();
            }
            
            MutatorStat amstat = aggregateMutatorStats();
            out.print("all");
            out.print("\t");
            out.print(amstat.pctMutatorCrypts());
            out.print("\t");
            out.print(amstat.pctMutatorCrypts50());
            out.print("\t");
            out.print(amstat.pctMutatorCrypts100());
            out.print("\t");
            out.print(amstat.pctMutatorCrypts100StErr());
            out.println();
            
            out.flush();
            out.close();
            
            
            // mutator inactivations
            fname = filename("_tsggraph.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);

            out.print("data type");
            out.print("\t");
            out.print("cells per crypt");
            
            for (double dp : RunStats.getDelProps(validStats()))
            {
                out.print("\t");
                out.print(dp);
            }
            out.println();
            
            // pct mutator inactivations
            for (int nc : RunStats.getNumCells(validStats()))
            {
                List<Double> vals = new ArrayList<Double>();
                List<Double> errs = new ArrayList<Double>();
                
                for (int dp : RunStats.getDelProps(validStats()))
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, dp);
                    
                    // 0 = non-mut inactivation, 1 = mut inactivation
                    List<Double> scores = new ArrayList<Double>();
                    
                    for (RunStats rs : rstats)
                    {
                        if (!rs.cancerous) continue;
                        
                        if (rs.mutMuts > 0)
                            scores.add(1.0);
                        else
                            scores.add(0.0);
                    }
                    
                    vals.add(Calc.mean(scores));
                    errs.add(Calc.stErr(scores));
                }
                
                out.print("pct mutator inactivations");
                out.print("\t");
                out.print(nc);
                for (double v : vals)
                {
                    out.print("\t");
                    out.print(v);
                }
                out.println();

                out.print("pct mutator inactivations err");
                out.print("\t");
                out.print(nc);
                for (double e : errs)
                {
                    out.print("\t");
                    out.print(e);
                }
                out.println();
            }

            // pct fixed mutator
            for (int nc : RunStats.getNumCells(validStats()))
            {
                List<Double> vals = new ArrayList<Double>();
                List<Double> errs = new ArrayList<Double>();
                
                for (int dp : RunStats.getDelProps(validStats()))
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, dp);
                    
                    // 0 = non-mut inactivation, 1 = mut inactivation
                    List<Double> scores = new ArrayList<Double>();
                    
                    for (RunStats rs : rstats)
                    {
                        double pct100 = (double)rs.mutatorCrypts100
                        /(double)rs.totalCrypts;

                        scores.add(pct100);
                    }
                    
                    vals.add(Calc.mean(scores));
                    errs.add(Calc.stErr(scores));
                }
                
                out.print("pct fixed mutator crypts");
                out.print("\t");
                out.print(nc);
                for (double v : vals)
                {
                    out.print("\t");
                    out.print(v);
                }
                out.println();
                
                out.print("pct fixed mutator crypts err");
                out.print("\t");
                out.print(nc);
                for (double e : errs)
                {
                    out.print("\t");
                    out.print(e);
                }
                out.println();
            }
            
            out.flush();
            out.close();

            
            // mutator inactivations
            fname = filename("_apoptsggraph.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("data type");
            out.print("\t");
            out.print("apoptosis rate");
            
            for (double dp : RunStats.getDelProps(validStats()))
            {
                out.print("\t");
                out.print(dp);
            }
            out.println();
            
            // pct fixed mutator
            for (double ar : RunStats.getApopRates(validStats()))
            {
                List<Double> vals = new ArrayList<Double>();
                List<Double> errs = new ArrayList<Double>();
                
                for (int dp : RunStats.getDelProps(validStats()))
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterApopRate(rstats, ar);
                    rstats = RunStats.filterDelProp(rstats, dp);
                    
                    // 0 = non-mut inactivation, 1 = mut inactivation
                    List<Double> scores = new ArrayList<Double>();
                    
                    for (RunStats rs : rstats)
                    {
                        double pct100 = (double)rs.mutatorCrypts100
                        /(double)rs.totalCrypts;
                        
                        scores.add(pct100);
                    }
                    
                    vals.add(Calc.mean(scores));
                    errs.add(Calc.stErr(scores));
                }
                
                out.print("pct fixed mutator crypts");
                out.print("\t");
                out.print(ar);
                for (double v : vals)
                {
                    out.print("\t");
                    out.print(v);
                }
                out.println();
                
                out.print("pct fixed mutator crypts err");
                out.print("\t");
                out.print(ar);
                for (double e : errs)
                {
                    out.print("\t");
                    out.print(e);
                }
                out.println();
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
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    List<RunStats> getStats(int $delProp)
    {
        List<RunStats> valids = validStats();
        List<RunStats> l = new ArrayList<RunStats>(valids.size());
        
        for (RunStats stats : valids)
        {
            if (stats.deleteriousRate() == $delProp)
                l.add(stats);
        }
        
        return l;
    }
    
    
    
    /**
     * Return mutator statistics broken out by del prop.
     *
     */
    class MutatorStat
    {
        int delProp = 0;
        
        int mutatorCrypts = 0;
        int mutatorCrypts50 = 0;
        int mutatorCrypts100 = 0;
        
        int totalCrypts = 0;
        
        List<Double> m100Pcts = new ArrayList<Double>();
        
        void incorporate(RunStats $stats)
        {
            totalCrypts += $stats.totalCrypts;
            
            mutatorCrypts += $stats.mutatorCrypts;
            mutatorCrypts50 += $stats.mutatorCrypts50;
            mutatorCrypts100 += $stats.mutatorCrypts100;
            
            double pct100 = (double)$stats.mutatorCrypts100
                /(double)$stats.totalCrypts;
            m100Pcts.add(pct100);
        }
        
        void incorporate(MutatorStat $stats)
        {
            totalCrypts += $stats.totalCrypts;
            
            mutatorCrypts += $stats.mutatorCrypts;
            mutatorCrypts50 += $stats.mutatorCrypts50;
            mutatorCrypts100 += $stats.mutatorCrypts100;

            double pct100 = (double)$stats.mutatorCrypts100
                /(double)$stats.totalCrypts;
            m100Pcts.add(pct100);
        }
        
        
        double pctMutatorCrypts()
        {
            return (double)mutatorCrypts/(double)totalCrypts;
        }

        double pctMutatorCrypts50()
        {
            return (double)mutatorCrypts50/(double)totalCrypts;
        }

        double pctMutatorCrypts100()
        {
            return (double)mutatorCrypts100/(double)totalCrypts;
        }
        
        double pctMutatorCrypts100StErr()
        {
            return Calc.stErr(m100Pcts);
        }
    }

    
    /**
     * Return mutator statistics broken out by del prop.
     *
     */
    public Collection<MutatorStat> mutatorStats()
    {
        Map<Integer,MutatorStat> mstats = new HashMap<Integer,MutatorStat>();
        for (RunStats stats : validStats())
        {
            int delprop = stats.deleteriousRate();

            MutatorStat mstat = mstats.get(delprop);
            if (mstat == null)
            {
                mstat = new MutatorStat();
                mstat.delProp = delprop;
                mstats.put(delprop, mstat);
            }
            
            mstat.incorporate(stats);
        }

        return mstats.values();
    }
    
    
    public MutatorStat aggregateMutatorStats()
    {
        MutatorStat mstat = new MutatorStat();
        for (RunStats stats : validStats())
        {
            mstat.incorporate(stats);
        }
        return mstat;
    }
    
}

package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


/**
 * CancerStat is an executable class that computes the cancer related 
 * statistics from XML final simulation state files.
 *
 */
public class CancerStat extends GenericStat
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
            System.err.println("Usage: java stemsim.statxml.CancerStat"
                               + " <data directory>");
        }
        
        // get the data directory from command line
        String resDirName = $args[0];
        File rdir = new File(resDirName);
        
        CancerStat stat = new CancerStat(rdir);
        stat.compile();
        stat.writeOutputFiles();
    }
    
    
    public CancerStat(File $dataDir)
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
            // write cancer stat output file
            fname = filename("_cstat.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);

            out.println("num sims");
            out.println(validStats().size());
            out.println();
            
            out.println("sim time mean");
            out.println(timeMean());
            out.println();
            
            out.println("percent surviving tissues");
            out.println(percentSurviving());
            out.println();
            
            out.println("percent living crypts (avg. across runs)");
            out.println(percentLivingCrypts());
            out.println();
            
            out.println("percent cancer tissues");
            out.println(percentCancer());
            out.println();
            
            out.println("percentage of cancer tissues with 2nd hit in TAC");
            out.println(percentTACCancer());
            out.println();
            
            out.println("count of cancer tissues with TAC knockout and no stem knockout");
            out.println(countTACNotStem());
            out.println();
            
            out.println("percentage of cancer tissues with mutator cancer");
            out.println(percentMutatorCancer());
            out.println();
            
            out.println("percent mutator stem cells in normal crypts (avg. across runs)");
            out.println(percentMutatorInNormalCrypts());
            out.println();
            
            out.println("percent mutator stem cells in cancer crypt (avg. across runs)");
            out.println(percentMutatorCellsInCancerCrypt());
            out.println();
            
            out.println("mean mutations in normal crypts per stem cell per time (avg. across runs)");
            out.println(mutationCountMean() * 100.0);
            out.println();
            
            out.println("stdev mutations in normal crypts per stem cell per time (avg. across runs)");
            out.println(mutationCountStDev() * 100.0);
            out.println();
            
            out.println("mean mutations in cancer crypts per stem cell per time (avg. across runs)");
            out.println(cancerCryptMutationCountMean() * 100.0);
            out.println();
            
            out.println("stdev mutations in cancer crypts per stem cell per time (avg. across runs)");
            out.println(cancerCryptMutationCountStDev() * 100.0);
            out.println();

            out.println("percent surviving by deleterious rate");
            out.println("rate\tpercent");
            double[][] t = pctSurvivingTable();
            for (int i=0; i<t.length; i++)
            {
                out.print(t[i][0]);
                out.print("\t");
                out.print(t[i][1]);
                out.println();
            }
            
            out.flush();
            out.close();
            
            
            // write Kaplan Myer output file
            fname = filename("_km.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("deleterious rate\t");
            out.print("apoptosis rate\t");
            out.print("mutation rate\t");
            out.print("mutator multiplier\t");
            out.print("stem cells per crypt\t");
            out.print("surviving\t");
            out.print("pct living crypts\t");
            out.print("cancer\t");
            out.print("time\t");
            out.print("tsg mutation rate\t");
            out.print("knockoutTAC\t");
            out.print("knockoutStem\t");
            out.print("ben apop muts\t");
            out.print("ben div muts\t");
            out.print("del apop muts\t");
            out.print("del div muts\t");
            out.print("mut muts\t");
            out.print("apop rate\t");
            out.print("feedback apop rate\t");
            out.print("symm div rate\t");
            out.print("asymm div rate\t");
            out.print("mut rate");
            out.println();
            for (RunStats stats : validStats())
            {
                double delRate = stats.deleteriousApopRate 
                + stats.deleteriousDivRate;
                out.print(dform.format(delRate));
                out.print("\t");

                out.print(dform.format(stats.apoptosisRate));
                out.print("\t");
                
                out.print(dform.format(stats.mutationRate));
                out.print("\t");
                
                out.print(dform.format(stats.mutatorMultiplier));
                out.print("\t");

                out.print(stats.stemcellsmean);
                out.print("\t");
                
                if (stats.livingCrypts > 0) out.print("yes");
                else out.print("no");
                out.print("\t");

                out.print(dform.format(stats.pctLivingCrypts()));
                out.print("\t");
                
                if (stats.cancerous) out.print("yes");
                else out.print("no");
                out.print("\t");
                
                out.print(tform.format(stats.time));
                out.print("\t");
                
                out.print(dform.format(stats.tsgMutationRate));
                out.print("\t");
                
                out.print(tform.format(stats.knockoutTAC));
                out.print("\t");
                out.print(tform.format(stats.knockoutStem));
                out.print("\t");

                out.print(stats.benApopMuts);
                out.print("\t");
                out.print(stats.benDivMuts);
                out.print("\t");
                out.print(stats.delApopMuts);
                out.print("\t");
                out.print(stats.delDivMuts);
                out.print("\t");
                out.print(stats.mutMuts);
                out.print("\t");

                out.print(dform.format(stats.apopRate));
                out.print("\t");
                out.print(dform.format(stats.feedbackApopRate));
                out.print("\t");
                out.print(dform.format(stats.divRate));
                out.print("\t");
                out.print(dform.format(stats.asymmDivRate));
                out.print("\t");
                out.print(dform.format(stats.mutRate));

                out.println();
            }
            
            out.flush();
            out.close();
            
            
            // param file
            fname = filename("_param.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            for (RunStats stats : validStats())
            {
                for (String key : stats.paramMap.keySet())
                {
                    String val = stats.paramMap.get(key);
                    out.print(key);
                    out.print("=");
                    out.print(val);
                    out.println();
                }
                
                break;
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
     * Return the ratio of mutator stem cells to normal stem cells in normal
     * (non-cancer) crypts.
     *
     */
    public double percentMutatorInNormalCrypts()
    {
        int totalStemCells = 0;
        int mutatorStemCells = 0;
        
        // list percentages by run
        List<RunStats> valid = validStats();
        List<Double> percentages = new ArrayList<Double>(valid.size());
        for (RunStats stats : valid)
        {
            int nonCancerMutatorCells = stats.stemCellsMutator 
            - stats.cancerCryptMutatorCells;
            
            int totalNonCancerCells = stats.totalStemCells 
                - stats.cancerCryptTotalCells;
            
            double p = (double)nonCancerMutatorCells
                /(double)totalNonCancerCells;
            
            if (!Double.isNaN(p))
            {
                percentages.add(p);
            }
        }
        
        // average across runs
        double total = 0.0;
        for (double p : percentages)
        {
            total += p;
        }
        
        return total / (double)percentages.size();
    }

    
    /**
     * Return the ratio of mutator stem cells to normal stem cells in the
     * cancer crypt.
     *
     */
    public double percentMutatorCellsInCancerCrypt()
    {
        List<RunStats> valid = validStats();
        
        int cancerRuns = 0;
        
        double pctMutator = 0.0;
        for (RunStats stats : valid)
        {
            if (stats.cancerous)
            {
                cancerRuns++;
                
                pctMutator += (double)stats.cancerCryptMutatorCells
                / (double)stats.cancerCryptTotalCells;
            }
        }
        
        return (double)pctMutator/(double)cancerRuns;
    }
    
    
    /**
     * Return a list of the number of mutations of each stem cell.
     *
     */
    public List<Integer> mutationCounts()
    {
        List<Integer> mutCounts = new ArrayList<Integer>(1000);
        for (RunStats stats : validStats())
        {
            mutCounts.addAll(stats.mutationCounts);
        }
        return mutCounts;
    }
    
    
    /**
     * Return a table of the ratio of living tissues per deleterious rate.
     *
     */
    public double[][] pctSurvivingTable()
    {
        Map<Integer,Integer> stc = new HashMap<Integer,Integer>();
        
        Map<Integer,Integer> tc = new HashMap<Integer,Integer>();
        
        List<RunStats> valid = validStats();
        SortedSet<Integer> dps = RunStats.getDelProps(valid);
        
        // initialize counts
        for (int i : dps)
        {
            stc.put(i, 0);
            tc.put(i, 0);
        }
        
        for (RunStats stats : valid)
        {
            int dr = stats.deleteriousRate();

            tc.put(dr, tc.get(dr)+1);
            if (stats.livingCrypts > 0)
            {
                stc.put(dr, stc.get(dr)+1);
            }
        }
        
        double[][] t = new double[dps.size()][2];
        int row = 0;
        for (int i : dps)
        {
            int surviving = stc.get(i);
            int total = tc.get(i);
            double ratio = 100.0 * (double)surviving / (double)total;
            if (Double.isNaN(ratio)) ratio = 0.0;
            
            t[row][0] = i;
            t[row][1] = ratio;
            
            row++;
        }
        
        return t;
    }
    
    
    
    /**
     * Return the ratio of living to all tissues.
     *
     */
    public double percentSurviving()
    {
        List<RunStats> valid = validStats();
        
        int totalRuns = valid.size();
        
        int livingRuns = 0;
        for (RunStats stats : valid)
        {
            if (stats.livingCrypts > 0)
            {
                livingRuns++;
            }
        }
        
        return (double)livingRuns/(double)totalRuns;
    }
    
    
    /**
     * Return the ratio of living to all crypts, averaged across runs.
     *
     */
    public double percentLivingCrypts()
    {
        double total = 0.0;
        int count = 0;
        
        for (RunStats stats : validStats())
        {
            double pct = (double) stats.livingCrypts
            / (double) stats.totalCrypts;

            total += pct;
            count++;
        }
        
        return (double)total / (double)count;
    }
    
    
    /**
     * Return the average finish time.
     *
     */
    public double timeMean()
    {
        int sum = 0;
        int count = 0;
        for (RunStats stats : validStats())
        {
            sum += stats.time;
            count ++;
        }
        return (double)sum/(double)count;
    }
        
    
    /**
     * Return the mean number of mutations per stem cell averaged across
     * simulation runs.
     *
     */
    public double mutationCountMean()
    {
        double total = 0.0;
        List<RunStats> valid = validStats();
        int count = 0;
        
        for (RunStats stats : valid)
        {
            double m = Calc.mean(stats.mutationCounts, stats.time);
            if (!Double.isNaN(m))
            {
                total += m;
                count++;
            }
        }
        
        return (double)total / (double)count;
    }
    
    
    /**
     * Return the standard deviation of the number of mutations per stem cell
     * averaged across runs.
     *
     */
    public double mutationCountStDev()
    {
        double total = 0.0;
        List<RunStats> valid = validStats();
        int count = 0;
        
        for (RunStats stats : valid)
        {
            double m = Calc.stDev(stats.mutationCounts, stats.time);
            if (!Double.isNaN(m))
            {
                total += m;
                count++;
            }
        }
        
        return (double)total / (double)count;
    }

    
    /**
     * Return the mean of the number of mutations per stem cell in the cancer
     * crypt averaged across simulation runs.
     *
     */
    public double cancerCryptMutationCountMean()
    {
        double total = 0.0;
        int count = 0;
        
        for (RunStats stats : validStats())
        {
            double m = Calc.mean(stats.cancerMutationCounts, stats.time);
            if (!Double.isNaN(m))
            {
                total += m;
                count++;
            }
        }
        
        return (double)total / (double)count;
    }
    
    
    /**
     * Return the standard deviation of the number of mutations per stem cell
     * in the cancer crypt averaged across runs.
     *
     */
    public double cancerCryptMutationCountStDev()
    {
        double total = 0.0;
        int count = 0;
        
        for (RunStats stats : validStats())
        {
            double m = Calc.stDev(stats.cancerMutationCounts, stats.time);
            if (!Double.isNaN(m))
            {
                total += m;
                count++;
            }
        }
        
        return (double)total / (double)count;
    }
    
    
    /**
     * Return the ratio of cancerous tissues to all tissues.
     *
     */
    public double percentCancer()
    {
        List<RunStats> valid = validStats();
        
        int totalRuns = valid.size();
        
        int cancerRuns = 0;
        for (RunStats stats : valid)
        {
            if (stats.cancerous)
            {
                cancerRuns++;
            }
        }
        
        return (double)cancerRuns/(double)totalRuns;
    }
    

    /**
     * Return the ratio of cancer runs with mutator to all cancer runs.
     *
     */
    public double percentMutatorCancer()
    {
        List<RunStats> valid = validStats();
        
        int mutatorCancerRuns = 0;
        int cancerRuns = 0;
        for (RunStats stats : valid)
        {
            if (stats.cancerous)
            {
                cancerRuns++;
                
                if (stats.mutMuts > 0)
                {
                    mutatorCancerRuns++;
                }
            }
        }
        
        return (double)mutatorCancerRuns/(double)cancerRuns;
    }

    
    /**
     * Return the percentage of cancer runs where the second TSG knockout was
     * in the TAC.
     *
     */
    public double percentTACCancer()
    {
        List<RunStats> valid = validStats();
        
        int tacCancerRuns = 0;
        int cancerRuns = 0;
        for (RunStats stats : valid)
        {
            if (stats.cancerous)
            {
                cancerRuns++;
                
                if (stats.knockoutTAC > 0.0
                    && stats.knockoutTAC <  stats.knockoutStem)
                {
                    tacCancerRuns++;
                }
            }
        }
        
        return (double)tacCancerRuns/(double)cancerRuns;
    }

    
    
    /**
     * Return count of TAC knockouts where there was no stem knockout.
     *
     */
    public int countTACNotStem()
    {
        // List<RunStats> valid = validStats();
        
        int count = 0;
        for (RunStats stats : _stats)
        {
            if (stats.knockoutTAC > 0.0
                && stats.knockoutStem < 0.0)
            {
                count++;
            }
        }
        
        return count;
    }
}

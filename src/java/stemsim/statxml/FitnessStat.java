package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


/**
 * FitnessStat is an executable class that computes the cancer related 
 * statistics from XML final simulation state files.
 *
 */
public class FitnessStat extends GenericStat
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    static public void main(String[] $args) throws Exception
    {
        if ($args.length < 1)
        {
            System.err.println("Usage: java stemsim.statxml.FitnessStat"
                               + " <data directory>");
        }
        
        // get the data directory from command line
        String resDirName = $args[0];
        File rdir = new File(resDirName);
        
        FitnessStat stat = new FitnessStat(rdir);
        stat.compile();
        stat.writeOutputFiles();
    }
    
    
    public FitnessStat(File $dataDir)
    {
        super($dataDir);
    }
    
    
    public void writeOutputFiles() throws Exception
    {
        // shared variables
        List<RunStats> validStats = validStats();
        SortedSet<Integer> ncs = RunStats.getNumCells(validStats);
        SortedSet<Integer> dps = RunStats.getDelProps(validStats);
        
        // write output files
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        String fname = null;
        try
        {
            // write crypt fitness measures
            fname = filename("_fit.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            // headers
            out.print("sim");
            out.print("\t");
            out.print("del prop");
            out.print("\t");
            out.print("pct living");
            for (CryptStats cd : validStats.get(0).cryptStats)
            {
                out.print("\t");
                out.print(cd.id);
            }
            out.println();
            
            int i=1;
            for (RunStats stats : validStats)
            {
                out.print(i++);

                out.print("\t");
                out.print(stats.deleteriousRate());
                
                out.print("\t");
                out.print(stats.pctLivingCrypts());
                
                for (CryptStats cd : stats.cryptStats)
                {
                    out.print("\t");
                    out.print(dform.format(cd.fitness()));
                }

                out.println();
            }
            
            out.flush();
            out.close();
            
            
            // avg fitness split by fixed mutator
            fname = filename("_fitavg.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("cells per crypt");
            out.print("\t");
            out.print("deleterious rate");
            out.print("\t");
            out.print("pct living");
            out.print("\t");
            out.print("fixed");
            out.print("\t");
            out.print("non-fixed");
            out.print("\t");
            out.print("fixed fitness");
            out.print("\t");
            out.print("non-fixed fitness");
            out.println();
            
            for (int nc : ncs)
            {
                List<RunStats> validnc = 
                    RunStats.filterNumCells(validStats, nc);

                for (int d : dps)
                {
                    List<RunStats> rstats = RunStats.filterDelProp(validnc, d);
                    
                    List<Double> pctLiving = new ArrayList<Double>();
                    List<Double> fixedFits = new ArrayList<Double>();
                    List<Double> nonFixedFits = new ArrayList<Double>();
                    
                    int numFixedMutatorCrypts = 0;
                    int numNonFixedMutatorCrypts = 0;
                    
                    for (RunStats rs : rstats)
                    {
                        pctLiving.add(rs.pctLivingCrypts());
                        
                        List<CryptStats> living = living(rs.cryptStats);
                        List<CryptStats> mutators = fixedMutators(living);
                        List<CryptStats> normals = nonFixedMutators(living);
                        
                        if (!mutators.isEmpty())
                        {
                            numFixedMutatorCrypts += mutators.size();
                            fixedFits.add(avgFitness(mutators));
                        }
                        
                        if (!normals.isEmpty())
                        {
                            numNonFixedMutatorCrypts += normals.size();
                            nonFixedFits.add(avgFitness(normals));
                        }
                    }
                    
                    out.print(nc);
                    out.print("\t");
                    out.print(d);
                    out.print("\t");
                    out.print(Calc.mean(pctLiving));
                    out.print("\t");
                    out.print(numFixedMutatorCrypts);
                    out.print("\t");
                    out.print(numNonFixedMutatorCrypts);
                    out.print("\t");
                    
                    if (fixedFits.isEmpty()) out.print("_");
                    else out.print(Calc.mean(fixedFits));
                    out.print("\t");
                    
                    if (nonFixedFits.isEmpty()) out.print("_");
                    else out.print(Calc.mean(nonFixedFits));
                    
                    out.println();
                }
            }
            
            out.flush();
            out.close();


            // avg fitness split by fixed mutator for cluster graphing
            fname = filename("_fitgraph.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("data type");
            out.print("\t");
            out.print("cells per crypt");

            for (int d : dps)
            {
                out.print("\t");
                out.print(d);
            }
            out.println();
            
            for (int nc : ncs)
            {
                // percent living crypts
                out.print("pct living");
                out.print("\t");
                out.print(nc);
                
                List<RunStats> validnc = 
                    RunStats.filterNumCells(validStats, nc);

                for (int d : dps)
                {
                    List<RunStats> rstats = RunStats.filterDelProp(validnc, d);

                    List<Double> pctLiving = new ArrayList<Double>();
                    
                    for (RunStats rs : rstats)
                    {
                        pctLiving.add(rs.pctLivingCrypts());
                    }
                    
                    out.print("\t");
                    out.print(Calc.mean(pctLiving));
                }
                
                out.println();
                
                
                // fitness
                out.print("all living crypts");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> fits = allFits(rstats);
                    
                    out.print("\t");
                    if (fits.isEmpty()) out.print("_");
                    else out.print(Calc.mean(fits));
                }
                
                out.println();
                
                out.print("all living crypts sterr");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> fits = allFits(rstats);
                    
                    out.print("\t");
                    if (fits.isEmpty()) out.print("_");
                    else out.print(Calc.stErr(fits));
                }
                
                out.println();
                
                
                // fixed fitness
                out.print("fixed mutator");
                out.print("\t");
                out.print(nc);

                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);

                    List<Double> fixedFits = fixedFits(rstats);

                    out.print("\t");
                    if (fixedFits.isEmpty()) out.print("_");
                    else out.print(Calc.mean(fixedFits));
                }

                out.println();

                out.print("fixed mutator sterr");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> fixedFits = fixedFits(rstats);
                    
                    out.print("\t");
                    if (fixedFits.isEmpty()) out.print("_");
                    else out.print(Calc.stErr(fixedFits));
                }
                
                out.println();
                
                
                // non-fixed fitness
                out.print("non-fixed mutator");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> nonFixedFits = nonFixedFits(rstats);
                    
                    out.print("\t");
                    if (nonFixedFits.isEmpty()) out.print("_");
                    else out.print(Calc.mean(nonFixedFits));
                }
                
                out.println();

                out.print("non-fixed mutator sterr");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> nonFixedFits = nonFixedFits(rstats);
                    
                    out.print("\t");
                    if (nonFixedFits.isEmpty()) out.print("_");
                    else out.print(Calc.stErr(nonFixedFits));
                }
                
                out.println();
                
                
                // cancer crypt fitness
                out.print("cancer crypt");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> fits = cancerCryptFits(rstats);
                    
                    out.print("\t");
                    if (fits.isEmpty()) out.print("_");
                    else out.print(Calc.mean(fits));
                }
                
                out.println();
                
                out.print("cancer crypt sterr");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> fits = cancerCryptFits(rstats);
                    
                    out.print("\t");
                    if (fits.isEmpty()) out.print("_");
                    else out.print(Calc.stErr(fits));
                }
                
                out.println();
                
                // cancer cell fitness
                out.print("cancer cell");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> fits = cancerCellFits(rstats);
                    
                    out.print("\t");
                    if (fits.isEmpty()) out.print("_");
                    else out.print(Calc.mean(fits));
                }
                
                out.println();
                
                out.print("cancer cell sterr");
                out.print("\t");
                out.print(nc);
                
                for (int d : dps)
                {
                    List<RunStats> rstats = validStats();
                    rstats = RunStats.filterNumCells(rstats, nc);
                    rstats = RunStats.filterDelProp(rstats, d);
                    
                    List<Double> fits = cancerCellFits(rstats);
                    
                    out.print("\t");
                    if (fits.isEmpty()) out.print("_");
                    else out.print(Calc.stErr(fits));
                }
                
                out.println();
            }
            
            out.flush();
            out.close();
            
            
            // grids of fitness
            fname = filename("_fitgrids.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            for (RunStats stats : validStats)
            {
                out.print("num cells");
                out.print("\t");
                out.print(stats.stemcellsmean);
                out.println();

                out.print("del prop");
                out.print("\t");
                out.print(stats.deleteriousRate());
                out.println();
                
                out.println();

                // set up counters and mod (cols)
                int rsi = 1; 
                int mod = 0;
                
                switch (stats.cryptStats.size()) 
                {
                    case 25:  mod = 5; break;
                    case 180:  mod = 18; break;
                    case 90:  mod = 9; break;
                    case 36:  mod = 6; break;
                    case 18:  mod = 3; break;
                    case 9:  mod = 3; break;
                    case 6:  mod = 3; break;
                    case 2:  mod = 2; break;
                    case 1:  mod = 1; break;
                        
                    default: 
                        throw new IllegalArgumentException(
                                                  "" + stats.cryptStats.size()); 
                }
                
                // inactivation
                out.println("inactivation");
                rsi = 1;
                for (CryptStats cs : stats.cryptStats)
                {
                    if (cs.cancer)
                        out.print("1");
                    else
                        out.print("0");
                    out.print("\t");
                    
                    if (rsi++ % mod  == 0)
                        out.println();
                }
                out.println();
                
                // mutators
                out.println("mutator");
                rsi = 1;
                for (CryptStats cs : stats.cryptStats)
                {
                    if (cs.fixedMutator)
                        out.print("1");
                    else
                        out.print("0");
                    out.print("\t");
                    
                    if (rsi++ % mod  == 0)
                        out.println();
                }
                out.println();

                // fitness rounded
                out.println("fitness rounded");
                rsi = 1;
                for (CryptStats cs : stats.cryptStats)
                {
                    double fit = cs.fitness() * 1000d;
                    
                    out.print(Math.round(fit));
                    out.print("\t");
                    
                    if (rsi++ % mod  == 0)
                        out.println();
                }
                out.println();

                // fitness
                out.println("fitness");
                rsi = 1;
                for (CryptStats cs : stats.cryptStats)
                {
                    out.print(dform.format(cs.fitness()));
                    out.print("\t");
                    
                    if (rsi++ % mod  == 0)
                        out.println();
                }
                out.println();
            }
            
            out.flush();
            out.close();
            
            
            
            // one row per tissue
            fname = filename("_fitvals1.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("data type");
            out.print("\t");
            out.print("num cells");
            out.print("\t");
            out.print("del prop");
            
            i = 0;
            for (CryptStats cs : validStats.get(1).cryptStats)
            {
                out.print("\t");
                out.print("c" + ++i);
            }
            
            out.println();
            
            for (RunStats stats : validStats)
            {
                // mutator
                out.print("mutator");
                out.print("\t");
                out.print(stats.stemcellsmean);
                out.print("\t");
                out.print(stats.deleteriousRate());

                for (CryptStats cs : stats.cryptStats)
                {
                    out.print("\t");

                    if (cs.fixedMutator)
                        out.print("1");
                    else
                        out.print("0");
                }

                out.println();
                
                // fitness
                out.print("fitness");
                out.print("\t");
                out.print(stats.stemcellsmean);
                out.print("\t");
                out.print(stats.deleteriousRate());
                
                for (CryptStats cs : stats.cryptStats)
                {
                    out.print("\t");
                    out.print(dform.format(cs.fitness()));
                }
                
                out.println();
                
            }
            
            out.flush();
            out.close();
			
			
			// one row per crypt
            fname = filename("_fitvals2.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            out.print("num cells");
            out.print("\t");
            out.print("del prop");
            out.print("\t");
            out.print("crypt");
            out.print("\t");
            out.print("mutator");
            out.print("\t");
            out.print("inactivation");
            out.print("\t");
            out.print("fitness");
            out.println();
            
            for (RunStats stats : validStats)
            {
                for (CryptStats cs : stats.cryptStats)
                {
					out.print(stats.stemcellsmean);
					out.print("\t");
					out.print(stats.deleteriousRate());

                    out.print("\t");
					out.print(cs.id);
					
                    out.print("\t");
                    if (cs.fixedMutator)
                        out.print("1");
                    else
                        out.print("0");

                    out.print("\t");
                    if (cs.cancer)
                        out.print("1");
                    else
                        out.print("0");
                    
                    out.print("\t");
                    out.print(dform.format(cs.fitness()));

					out.println();
                }
            }
            
            out.flush();
            out.close();
            
            
            // hex points
            fname = filename("_hexpoints.txt");
            outfile = new File(fname);
            fw = new FileWriter(outfile);
            out = new PrintWriter(fw);
            
            for (RunStats stats : validStats)
            {
                List<CryptStats> cs = stats.cryptStats;
                CryptStats.setHexPoints(cs);
                
                int nr = CryptStats.numRows(cs);

                // coordinates
                for (int ci=0; ci<nr; ci++)
                {
                    List<CryptStats> row = CryptStats.getRow(cs, ci);
                    for (CryptStats c : row)
                    {
                        out.print(c.getHexPoint());
                        out.print(" ");
                    }
                    out.println();
                }
                out.println();
                
                // distance
                CryptStats topleft = CryptStats.getRow(cs, 0).get(0);
                for (int ci=0; ci<nr; ci++)
                {
                    List<CryptStats> row = CryptStats.getRow(cs, ci);
                    for (CryptStats c : row)
                    {
                        out.print(c.hexDistance(topleft));
                        out.print(" ");
                    }
                    out.println();
                }
                out.println();
                
                // distance classes
                /*
                for (DistanceClass dc : distanceClasses(cs))
                {
                    out.println(dc);
                }
                 */
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
    
    
    static double avgFitness(List<CryptStats> $stats)
    {
        double f = 0.0;
        int n = 0;
        
        for (CryptStats cs : $stats)
        {
            if (cs.numCells <= 0) continue;
            
            n++;
            f += cs.fitness();
        }
     
        return f / (double) n;
    }
    
    
    static List<CryptStats> fixedMutators(List<CryptStats> $stats)
    {
        List<CryptStats> l = new ArrayList<CryptStats>($stats.size());
        
        for (CryptStats cs : $stats)
        {
            if (cs.fixedMutator) l.add(cs);
        }
        
        return l;
    }

    
    static List<CryptStats> nonFixedMutators(List<CryptStats> $stats)
    {
        List<CryptStats> l = new ArrayList<CryptStats>($stats.size());
        
        for (CryptStats cs : $stats)
        {
            if (!cs.fixedMutator) l.add(cs);
        }
        
        return l;
    }

    
    static List<CryptStats> living(List<CryptStats> $stats)
    {
        List<CryptStats> l = new ArrayList<CryptStats>($stats.size());
        
        for (CryptStats cs : $stats)
        {
            if (cs.numCells > 0) l.add(cs);
        }
        
        return l;
    }
    
    
    static List<CryptStats> cancer(List<CryptStats> $stats)
    {
        List<CryptStats> l = new ArrayList<CryptStats>($stats.size());
        
        for (CryptStats cs : $stats)
        {
            if (cs.cancer) l.add(cs);
        }
        
        return l;
    }
    
    
    static List<Double> allFits(List<RunStats> $stats)
    {
        List<Double> fits = new ArrayList<Double>();
        
        for (RunStats rs : $stats)
        {
            List<CryptStats> living = living(rs.cryptStats);
            
            if (!living.isEmpty())
            {
                fits.add(avgFitness(living));
            }
        }
        
        return fits;
    }

    static List<Double> fixedFits(List<RunStats> $stats)
    {
        List<Double> fixedFits = new ArrayList<Double>();
        
        for (RunStats rs : $stats)
        {
            List<CryptStats> living = living(rs.cryptStats);
            List<CryptStats> mutators = fixedMutators(living);
            
            if (!mutators.isEmpty())
            {
                fixedFits.add(avgFitness(mutators));
            }
        }
        
        return fixedFits;
    }
    
    
    static List<Double> nonFixedFits(List<RunStats> $stats)
    {
        List<Double> nonFixedFits = new ArrayList<Double>();
        
        for (RunStats rs : $stats)
        {
            List<CryptStats> living = living(rs.cryptStats);
            List<CryptStats> normals = nonFixedMutators(living);
            
            if (!normals.isEmpty())
            {
                nonFixedFits.add(avgFitness(normals));
            }
        }
        
        return nonFixedFits;
    }
    
    
    static List<Double> cancerCryptFits(List<RunStats> $stats)
    {
        List<Double> fits = new ArrayList<Double>();
        
        for (RunStats rs : $stats)
        {
            List<CryptStats> ccs = cancer(rs.cryptStats);
            
            if (!ccs.isEmpty())
            {
                fits.add(avgFitness(ccs));
            }
        }
        
        return fits;
    }

    
    static List<Double> cancerCellFits(List<RunStats> $stats)
    {
        List<Double> fits = new ArrayList<Double>();
        
        for (RunStats rs : $stats)
        {
            if (rs.cancerous)
            {
                fits.add(rs.tsgCellFitness());
            }
        }
        
        return fits;
    }
}

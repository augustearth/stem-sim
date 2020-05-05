package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;



public class RunStats
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    static public List<RunStats> filterNumCells(List<RunStats> $stats, 
                                              int $numCells)
    {
        List<RunStats> rss = new ArrayList<RunStats>();
        
        for (RunStats rs : $stats)
        {
            if (rs.stemcellsmean == $numCells)
                rss.add(rs);
        }
        
        return rss;
    }
    
    static public List<RunStats> filterDelProp(List<RunStats> $stats, 
                                             int $delProp)
    {
        List<RunStats> l = new ArrayList<RunStats>($stats.size());
        
        for (RunStats stats : $stats)
        {
            if (stats.deleteriousRate() == $delProp)
                l.add(stats);
        }
        
        return l;
    }
    
    static public List<RunStats> filterApopRate(List<RunStats> $stats, 
                                                double $rate)
    {
        List<RunStats> l = new ArrayList<RunStats>($stats.size());
        
        for (RunStats stats : $stats)
        {
            if (stats.apoptosisRate == $rate)
                l.add(stats);
        }
        
        return l;
    }
    
    static public SortedSet<Integer> getDelProps(List<RunStats> $stats)
    {
        SortedSet<Integer> ss = new TreeSet<Integer>();
        for (RunStats stats : $stats)
        {
            ss.add(stats.deleteriousRate());
        }
        return ss;
    }
    
    static public SortedSet<Integer> getNumCells(List<RunStats> $stats)
    {
        SortedSet<Integer> ss = new TreeSet<Integer>();
        for (RunStats stats : $stats)
        {
            ss.add(stats.stemcellsmean);
        }
        return ss;
    }

    static public SortedSet<Double> getApopRates(List<RunStats> $stats)
    {
        SortedSet<Double> ss = new TreeSet<Double>();
        for (RunStats stats : $stats)
        {
            ss.add(stats.apoptosisRate);
        }
        return ss;
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Variables
    ////////////////////////////////////////////////////////////////////////////
    
    // all simulation parameters
    SortedMap<String,String> paramMap = new TreeMap<String,String>();
    
    // selected simulation parameters
    double simulationDuration = 0.0;
    double cancerthresh = 0.0;
    double deleteriousApopRate = 0.0;
    double deleteriousDivRate = 0.0;
    public int stemcellsmean = 0;
    double apoptosisRate = 0.0;
    double mutationRate = 0.0;
    double tsgMutationRate = 0.0;
    double mutatorMultiplier = 0.0;

    // time of last event
    double time = 0.0;
    
    // true if cancerous crypt exists in tissue
    boolean cancerous = false;
    
    // true if uncontrolled growth (old cancer)  in a crypt
    boolean outofcontrol = false;
    
    // times of tsg knockouts
    double knockoutStem = -1.0;
    double knockoutTAC = -1.0;
    
    // cancer cell fitness
    int benApopMuts = 0;
    int benDivMuts = 0;
    int delApopMuts = 0;
    int delDivMuts = 0;
    int mutMuts = 0;
    double apopRate = 0.0;
    double feedbackApopRate = 0.0;
    double divRate = 0.0;
    double asymmDivRate = 0.0;
    double mutRate = 0.0;
    
    // counts of crypts
    int totalCrypts = 0;
    int livingCrypts = 0;
    
    // counts of mutations in normal crypts
    List<Integer> mutationCounts = new LinkedList<Integer>();
    
    // counts of mutations in cancer crypt
    List<Integer> cancerMutationCounts = new LinkedList<Integer>();
    
    // total number of stem cells in tissue
    int totalStemCells = 0;
    
    // count of stem cells with mutator mutation
    int stemCellsMutator = 0;
 
    // count of mutator stem cells in cancer crypt with mutator mutation
    int cancerCryptMutatorCells = 0;
    
    // count of all stem cells in cancer crypt
    int cancerCryptTotalCells = 0;
    
    // count crypts with mutator cells
    int mutatorCrypts = 0;
    int mutatorCrypts50 = 0;
    int mutatorCrypts100 = 0;
    
    // list of crypt fitness statistics
    List<CryptStats> cryptStats = new ArrayList<CryptStats>();

    // TSG cell stats
    double tsgcellApop = -1.0;
    double tsgcellDiv = -1.0;
    
    double tsgCellFitness()
    {
        return tsgcellDiv - tsgcellApop;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Methods
    ////////////////////////////////////////////////////////////////////////////
    int deleteriousRate()
    {
        float delA = (float)deleteriousApopRate * 100.0f;
        float delD = (float)deleteriousDivRate * 100.0f;
        
        return Math.round(delA + delD);
    }
    
    double pctLivingCrypts()
    {
        return (double)livingCrypts/(double)totalCrypts;
    }
    
}

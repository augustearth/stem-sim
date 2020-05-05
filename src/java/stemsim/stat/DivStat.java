package stemsim.stat;


import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import stemsim.event.*;
import stemsim.object.*;
import stemsim.simulation.*;
import stemsim.util.*;
import stemsim.statxml.XMLResultParser;
import stemsim.statxml.RunStats;


/**
 * DivStat monitors crypts and records their stem cell population size as a
 * function of time.
 *
 */
public class DivStat extends SimEventListenerAdapter
{
    static DecimalFormat dform = new DecimalFormat("#.##");
    static DecimalFormat lform = new DecimalFormat("#.######");


    /**
     * Result file processor.
     *
     */
    static public void main(String[] $args) throws Exception
    {
        String fstr = $args[0];
        File dataDir = new File(fstr);
        
        // recursively find all divstat.txt files in data directory
        FilenameFilter filter = new FileUtil.ExactMatchFilter("divstat.txt");
        List<File> rfiles = new ArrayList<File>();
        FileUtil.recursiveGetFiles(dataDir, filter, rfiles);
        
        // result data structure
        Result result = new Result(dataDir);
        
        // process files
        for (File f : rfiles)
        {
            process(result, f);
        }
        
        // write output file
        outputSummary(result);
        outputGraphData(result);
    }
        
    
    
    /**
     * Result file processor.
     *
     */
    static void process(Result $result, File $file)
    throws FileNotFoundException, IOException, ParserConfigurationException,
    SAXException
    {
        Map<Double,Summary> smap = $result.getSummaryMap();
        
        // grab cells per crypt from local run.xml file
        File xmlfile = new File($file.getParent(), "run01_01.xml");
        String cellsParam = 
            FileUtil.getStemSimParameter(xmlfile, "crypt.numcells.mean");
        int cellsPerCrypt = -1;
        if (cellsParam != null)
        {
            cellsPerCrypt = Integer.parseInt(cellsParam);
        }
        
        BufferedReader in = new BufferedReader(new FileReader($file));
        String line = null;
        List<String> skipped = new LinkedList<String>();
        
        // skip the first line
        in.readLine();
        
        while ((line = in.readLine()) != null)
        {
            // System.err.println(line);
            
            String[] tokens = line.split("\\s");
            if (tokens.length != 7)
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
            
            // read values from current line
            double delprop = Double.parseDouble(tokens[1]);
            
            // get appropriate summary
            Summary s = smap.get(delprop);
            if (s == null)
            {
                s = new Summary(delprop);
                smap.put(delprop, s);
            }

            // parse values
            int sdivs = Integer.parseInt(tokens[2]);
            int asdivs = Integer.parseInt(tokens[3]);
            
            double smutrate = Double.parseDouble(tokens[4]);
            double asmutrate = Double.parseDouble(tokens[5]);
            double time = Double.parseDouble(tokens[6]);
            
            // incorporate into our stats
            s.numcells = cellsPerCrypt;
            s.totalSymmDivs += sdivs;
            s.totalAsymmDivs += asdivs;
            
            s.symmMutRate += smutrate;
            s.asymmMutRate += asmutrate;
            
            double tdivs = sdivs + asdivs;
            double tpt = tdivs / time;
            s.divsPerTime += tpt;
            
            double em = ((double)sdivs * smutrate) 
                + ((double)asdivs * asmutrate);
            double mpt = em / time;
            
            s.expectedMuts += em;
            s.mutsPerTime += mpt;
            
            s.count++;
            
            // store Row
            Row r = new Row();
            r.numcells = cellsPerCrypt;
            r.delProp = delprop;
            r.symmDivs = sdivs;
            r.asymmDivs = asdivs;
            r.symmMutRate = smutrate;
            r.asymmMutRate = asmutrate;
            r.time = time;
            $result.add(r);
        }
        
        in.close();
        
        if (skipped.size() > 0)
        {
            System.err.println("skipped lines");
            for (String sk : skipped)
            {
                System.err.println(sk);
            }
        }
        
    }
    
    
    static void outputGraphData(Result $result)
    throws IOException
    {
        PrintWriter out = new PrintWriter(
                         new FileWriter($result.getOutputFile("divstatgraph")));

        // header line
        out.print("data");
        out.print("\t");
        out.print("cells");

        for (double dp : $result.getDelProps())
        {
            out.print("\t");
            out.print(Math.round(dp * 100));
        }
        out.println();
        
        Map<Row,Double> vals = null;

        // divs per time
        vals = new HashMap<Row,Double>();
        for (Row r : $result.getRows()) vals.put(r, r.divsPerTime());
        graphData(out, $result, vals, "divs per time");
        
        // muts per time
        vals = new HashMap<Row,Double>();
        for (Row r : $result.getRows()) vals.put(r, r.mutsPerTime());
        graphData(out, $result, vals, "muts per time");
        
        out.close();
    }        
    
    
    static void graphData(PrintWriter out, 
                          Result $result, 
                          Map<Row,Double> $vals,
                          String $label)
    throws IOException, FileNotFoundException
    {
        for (int nc : $result.getNumCells())
        {
            out.print($label);
            out.print("\t");
            out.print(nc);
            
            for (double dp : $result.getDelProps())
            {
                List<Row> rows = $result.getRows();
                rows = filterNumCells(rows, nc);
                rows = filterDelProp(rows, dp);
                
                List<Double> dpt = new ArrayList<Double>();
                for (Row r : rows)
                {
                    dpt.add($vals.get(r));
                }
                
                out.print("\t");
                out.print(Calc.mean(dpt));
            }
            out.println();
        }
        
        for (int nc : $result.getNumCells())
        {
            out.print($label + " error");
            out.print("\t");
            out.print(nc);
            
            for (double dp : $result.getDelProps())
            {
                List<Row> rows = $result.getRows();
                rows = filterNumCells(rows, nc);
                rows = filterDelProp(rows, dp);
                
                List<Double> dpt = new ArrayList<Double>();
                for (Row r : rows)
                {
                    dpt.add($vals.get(r));
                }
                
                out.print("\t");
                out.print(Calc.stErr(dpt));
            }
            out.println();
        }
        
    }
    
    
    static void outputSummary(Result $result)
    throws IOException
    {
        PrintWriter out = new PrintWriter(
                        new FileWriter($result.getOutputFile("divstatsum")));
        out.print("num cells");
        out.print("\t");
        out.print("del prop");
        out.print("\t");
        out.print("total symmetric divs");
        out.print("\t");
        out.print("total asymmetric divs");
        out.print("\t");
        out.print("avg divs per time");
        out.print("\t");
        out.print("expected muts");
        out.print("\t");
        out.print("avg muts per time");
        out.print("\t");
        out.print("avg symm mut rate");
        out.print("\t");
        out.print("avg asymm mut rate");
        out.println();
        
        for (Double d : $result.getSummaryMap().keySet())
        {
            Summary s = $result.getSummaryMap().get(d);

            out.print(s.numcells);
            
            out.print("\t");
            out.print(d);
            
            out.print("\t");
            out.print(s.totalSymmDivs);
            
            out.print("\t");
            out.print(s.totalAsymmDivs);
            
            out.print("\t");
            out.print(s.avgDivsPerTime());
            
            out.print("\t");
            out.print(s.expectedMuts);
            
            out.print("\t");
            out.print(s.avgMutsPerTime());

            out.print("\t");
            out.print(s.avgSymmMutRate());

            out.print("\t");
            out.print(s.avgAsymmMutRate());

            out.println();
        }
        out.close();
    }
    
    
    static class Result
    {
        File _source = null;
        Map<Double,Summary> _smap = new HashMap<Double,Summary>();
        List<Row> _rows = new ArrayList<Row>();    
        
        public Result(File $source)
        {
            _source = $source;
        }
        
        public Map<Double,Summary> getSummaryMap()
        {
            return _smap;
        }
        
        public List<Row> getRows()
        {
            return _rows;
        }
        
        public void add(Row $row)
        {
            _rows.add($row);
        }
        
        File getOutputFile(String $suffix)
        {
            String fname = _source.getName() + "_" + $suffix + ".txt";
            return new File(fname);
        }
        
        public SortedSet<Double> getDelProps()
        {
            SortedSet ss = new TreeSet<Double>();
            for (Row r : _rows)
            {
                ss.add(r.delProp);
            }
            return ss;
        }

        public SortedSet<Integer> getNumCells()
        {
            SortedSet ss = new TreeSet<Integer>();
            for (Row r : _rows)
            {
                ss.add(r.numcells);
            }
            return ss;
        }
    }
    
    
    static class Summary
    {
        public Summary(double delProp)
        {
            this.delProp = delProp;
        }
        
        int numcells = -1;
        double delProp = 0.0;
        
        long totalSymmDivs = 0;
        long totalAsymmDivs = 0;
        double divsPerTime = 0.0;
        
        double expectedMuts = 0.0;
        double mutsPerTime = 0.0;
        
        double symmMutRate = 0.0;
        double asymmMutRate = 0.0;
        
        int count = 0;
        
        double avgDivsPerTime()
        {
            return divsPerTime / (double)count;
        }
        
        double avgMutsPerTime()
        {
            return mutsPerTime / (double)count;
        }
        
        double avgSymmMutRate()
        {
            return symmMutRate / (double)count;
        }

        double avgAsymmMutRate()
        {
            return asymmMutRate / (double)count;
        }
    }
    
    
    static class Row
    {
        int numcells = -1;
        double delProp = 0.0;
        
        long symmDivs = 0;
        long asymmDivs = 0;

        double symmMutRate = 0.0;
        double asymmMutRate = 0.0;

        double time = 0.0;
        
        public double muts()
        {
            return ((double)symmDivs * symmMutRate)
            + ((double)asymmDivs * asymmMutRate);
        }
        
        public double mutsPerTime()
        {
            return muts() / time;
        }
        
        public long divs()
        {
            return symmDivs + asymmDivs;
        }
        
        public double divsPerTime()
        {
            return divs() / time;
        }
        
        public String toString()
        {
            return numcells
            + " " + delProp
            + " " + symmDivs
            + " " + asymmDivs
            + " " + symmMutRate
            + " " + asymmMutRate
            + " " + time
            ;
        }
    }
    
    
    static List<Row> filterNumCells(List<Row> $rows, 
                                    int $numCells)
    {
        List<Row> rs = new ArrayList<Row>();
        
        for (Row r : $rows)
        {
            if (r.numcells == $numCells)
                rs.add(r);
        }
        
        return rs;
    }
    
    static List<Row> filterDelProp(List<Row> $rows, 
                                   double $delProp)
    {
        List<Row> rs = new ArrayList<Row>();
        
        for (Row r : $rows)
        {
            if (r.delProp == $delProp)
                rs.add(r);
        }
        
        return rs;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    int _symmDivs = 0;
    int _asymmDivs = 0;
    
    double _sumSymmMutRate = 0.0;
    double _sumAsymmMutRate = 0.0;


    /**
     * Start of new simulation
     *
     */ 
    public void start(SimulationStartEvent $event) 
    {
        _symmDivs = 0;
        _asymmDivs = 0;
        _sumSymmMutRate = 0.0;
        _sumAsymmMutRate = 0.0;
    }
    
    
    public void division(SymmetricDivisionEvent $event)
    {
        StemCell subject = $event.getSubject();

        _symmDivs++;
        
        _sumSymmMutRate += subject.getMutationRate();
    }
    
    
    public void asymmetricDivision(AsymmetricDivisionEvent $event)
    {
        StemCell subject = $event.getSubject();
        
        _asymmDivs++;
        
        _sumAsymmMutRate += subject.getMutationRate();
    }
    
    
    /**
     * At the end of the simulation, write the output file.
     *
     */
    public void end(SimulationEndEvent $event)
    {
        System.err.println("END");

        Simulation sim = $event.getSubject();
        SimulationParams params = sim.getParams();
        
        double delprop = params.getDouble("mutation.rate.deleterious.apop");
        delprop += params.getDouble("mutation.rate.deleterious.div");

        double time = $event.getTime();
        
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        
        try
        {
            outfile = new File(sim.getOutputDirectory(), 
                               "divstat.txt");
            
            if (outfile.exists())
            {
                fw = new FileWriter(outfile, true);
                out = new PrintWriter(fw);
            }
            else
            {
                fw = new FileWriter(outfile);
                out = new PrintWriter(fw);
                
                // header
                out.print("sim");
                out.print("\t");
                out.print("del prop");
                out.print("\t");
                out.print("symm divs");
                out.print("\t");
                out.print("asymm divs");
                out.print("\t");
                out.print("avg symm mut rate");
                out.print("\t");
                out.print("avg asymm mut rate");
                out.print("\t");
                out.print("time");
                out.println();
            }
            
            out.print(sim.getLabel());
            out.print("\t");
            
            // compute data
            double avgSymmMut = _sumSymmMutRate / (_symmDivs);
            double avgAsymmMut = _sumAsymmMutRate / (_asymmDivs);

            // write data
            out.print(dform.format(delprop));
            out.print("\t");
            out.print(_symmDivs);
            out.print("\t");
            out.print(_asymmDivs);
            out.print("\t");
            out.print(lform.format(avgSymmMut));
            out.print("\t");
            out.print(lform.format(avgAsymmMut));
            out.print("\t");
            out.print(dform.format(time));
            out.println();
            
            out.flush();
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("could not write: " + outfile);
            e.printStackTrace();
        }
    }
    
    
    public boolean equals(Object $obj)
    {
        if ($obj == null) return false;
        
        return ($obj instanceof DivStat); 
    }
    
    public int hashCode()
    {
        return 0;
    }
}
package stemsim.simulation;


import java.io.*;
import java.text.*;
import java.util.*;
import stemsim.event.*;
import stemsim.object.*;


/**
 * SimulationRunner is an executable class that runs understands parameater
 * sweeps and runs simulations appropriately.
 *
 */
public class SimulationRunner
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** The output directory in which to place data files */
    static File _outputDir = null;
    
    /** Formatter for text output */
    static DateFormat datef = new SimpleDateFormat("MM.dd.yy_HH.mm.ss");
    
    
    /**
     * Main method executable from command line.
     *
     */
    public static void main(String[] $args) throws Exception
    {
        if ($args.length < 1)
        {
            usage();
            System.exit(1);
        }

        File paramFile = new File($args[0]);
        SimulationParams params = SimulationParams.load(paramFile);
        
        String based = datef.format(new Date());
        _outputDir = new File(based);
        
        int i=1;
        while (_outputDir.exists())
        {
            _outputDir = new File(based + "." + i++);
        }
        _outputDir.mkdir();

        String sweepParam = params.getString("runset.parametersweep.parameter");
        if (sweepParam != null && sweepParam.trim().length() > 0)
        {
            runSweep(params);
        }
        else
        {
            run(params);
        }
    }
    
    
    /**
     * Runs a parameter sweep based on parameter sweep information set in
     * simulation properties file.
     *
     */
    public static void runSweep(SimulationParams $params)
    throws Exception
    {
        String sweepParam = 
            $params.getString("runset.parametersweep.parameter");
        double sweepMin = $params.getDouble("runset.parametersweep.min");
        double sweepMax = $params.getDouble("runset.parametersweep.max");
        double sweepStep = $params.getDouble("runset.parametersweep.step");
        
        // run all but last iteration
        for (double d = (sweepStep > 0.0) ? sweepMin : sweepMax;
             (sweepStep > 0.0) ? d<sweepMax : d>sweepMin;
             d+=sweepStep)
        {
            $params.setProperty(sweepParam, String.valueOf(d));
            System.err.println(sweepParam + " = " + String.valueOf(d));
            
            run($params);
        }
        
        // run last iteration at max sweep val
        // given the fuzzy nature of floating point comparison, safest to do
        // it this way
        double lastVal = (sweepStep > 0.0) ? sweepMax : sweepMin;
        $params.setProperty(sweepParam, String.valueOf(lastVal));
        run($params);
    }
    
    
    /**
     * Run a simulation.
     *
     */
    public static void run(SimulationParams $params) 
    throws Exception
    {
        Simulation simulation = null;
        
        // get simulation type from simulation params
        String simtype = $params.getString("simulation.type");
        if (simtype.equals("crypt")) // Running a single crypt
        {
            simulation = new CryptSimulation();
        }
        else if (simtype.equals("tissue"))
        {
            simulation = new TissueSimulation();
        }
        else
        {
            throw new IllegalArgumentException("unrecognized simulation.type");
        }

        // set the params and output directory
        simulation.setParams($params);
        simulation.setOutputDirectory(_outputDir);

        // run the simulation with the given parameters a number of times
        // configured in the simulation params
        final int numRuns = $params.getInt("runset.runs");
        for (int i=0; i<numRuns; i++)
        {
            String is = String.valueOf(i + 1);
            if (i < 9)
            {
                is = "0" + is;
            }
            String fname = "run" + simulation.getId() + "_" + is + ".xml";

            File xmlfile = new File(_outputDir, fname);
            FileWriter fw = new FileWriter(xmlfile);
            PrintWriter xml = new PrintWriter(fw);
            
            xml.println("<simulation>");
            xml.println();
            xml.println($params.toXML());
            xml.println();
            
            simulation.setLabel(is);
            simulation.run();

            xml.println(simulation.toXML());

            xml.println("</simulation>");
            xml.println();
            xml.flush();
        }
        
    }

    
    /**
     * Echo usage to standard error.
     *
     */
    public static void usage()
    {
        System.out.println("usage: SimulationRunner <param.file>");
    }
}

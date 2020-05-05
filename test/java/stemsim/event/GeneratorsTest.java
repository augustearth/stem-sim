package stemsim.event;


import java.io.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import stemsim.simulation.*;
import stemsim.object.*;
import stemsim.event.*;


public class GeneratorsTest extends TestCase 
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    static public void testExponential(double $lambda, 
                                       double $floor,
                                       double[] $times, 
                                       String $fname)
        throws IOException
    {
        // write output summary
        String resDir = System.getProperty("testresults.dir");
        File outFile = new File(resDir, $fname);
        PrintWriter out = new PrintWriter(new FileWriter(outFile));
        
        out.format("lambda\t%f\n\n", $lambda);
        
        double texp = (1/$lambda) + $floor;
        double tvar = 1/Math.pow($lambda, 2);
        double tdev = Math.sqrt(1/Math.pow($lambda, 2));
        out.println("theoretical values");
        out.format("E\t%f\n", texp);
        out.format("Var\t%f\n", tvar);
        out.format("StDev\t%f\n\n", tdev);
        
        double min = $times[min($times)];
        double max = $times[max($times)];
        
        out.println("actual values");
        out.format("min\t%f\n", min);
        out.format("min\t%f\n", max);
        
        int numbins = 40;
        int[] bins = new int[numbins];
        double chunk = (max - min) / (double)numbins;
        
        for (int i=0; i<$times.length; i++)
        {
            double time = $times[i];
            double bin = Math.floor((time - min)/chunk);
            bins[Math.min(bins.length - 1, (int)bin)]++;
        }
        
        out.format("average\t%f\n", average($times));
        out.format("variance\t%f\n", variance($times));
        out.format("stdev\t%f\n", stdev($times));
        
        out.println();
        out.println("time\tcount");
        for (int i=0; i<bins.length; i++)
        {
            out.print(min + (i * chunk));
            out.print("\t");
            out.println(bins[i]);
        }
        
        out.flush();
        out.close();
        
        
        // check that actual value are within threshold of theoretical values
        double thresh = 0.075;
        assertTrue(Math.abs(texp - average($times)) < texp * thresh);
        assertTrue(Math.abs(tvar - variance($times)) < tvar * thresh);
        assertTrue(Math.abs(tdev - stdev($times)) < tdev * thresh);
    }


    static public int min(double[] $nums)
    {
        int least = 0;
        for (int i=1; i<$nums.length; i++)
        {
            if ($nums[i] < $nums[least])
            {
                least = i;
            }
        }
        return least;
    }


    static public int max(double[] $nums)
    {
        int max = 0;
        for (int i=1; i<$nums.length; i++)
        {
            if ($nums[i] > $nums[max])
            {
                max = i;
            }
        }
        return max;
    }


    static public double average(double[] $nums)
    {
        int total = 0;
        for (int i=1; i<$nums.length; i++)
        {
            total += $nums[i];
        }
        return total / (double)$nums.length;
    }

    static double stdev(double[] $nums)
    {
        double variance = variance($nums);
        double stdev = Math.sqrt(variance);
        
        return stdev;
    }

    static double variance(double[] $nums)
    {
        double mean = average($nums);
        double sumofsquares = 0.0;
        int count = 0;
        for (double mc : $nums)
        {
            double d = mc - mean;
            d = d * d;
            sumofsquares += d;
            count ++;
        }
        
        double variance = sumofsquares/(double)count;
        
        return variance;
    }
        
    
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    SimulationParams _params;
    Simulation _simulation;
    Crypt _crypt;
    StemCell _cell;

	protected void setUp() 
    {
        _params = new SimulationParams();
        _params.setProperty("crypt.numcells.mean", "1");
        _params.setProperty("crypt.numcells.standarddeviation", "1");

        _params.setProperty("stemcell.tacsize.base", "100");

        _params.setProperty("stemcell.asymmetricdivision.ratio", "20");

        _params.setProperty("stemcell.divisionrate.base", "0.05");
        _params.setProperty("crypt.division.multiplier", "1");
        _params.setProperty("crypt.division.deadneighbor.multiplier", "1");
        _params.setProperty("event.celldivision.floor", "0");

        _params.setProperty("stemcell.apoptosisrate.base", "0.05");
        _params.setProperty("crypt.apoptosis.multiplier", "1");
        _params.setProperty("event.apoptosis.floor", "0");
        
        _simulation  = new TissueSimulation();
        _simulation.setParams(_params);
        
        _crypt = new Crypt(_simulation, 0);
        
        _cell = StemCell.create(_crypt);
        
	}
    

	public void testCellDivision() throws IOException
    {
        SymmetricDivisionEvent cde = null;
        int num = 20000;
        double[] times = null;
        double floor = 0.0;

        // no floor
        times = new double[num];
        for (int i=0; i<num; i++)
        {
            cde = SymmetricDivisionEvent.generate(0.0, _cell);
            times[i] = cde.getTime();
        }
        testExponential(_cell.getDivisionRate(), floor, times, "CellDiv.txt");

        // with floor
        floor = 4.0;
        _params.setProperty("event.celldivision.floor", "" + floor);
        _simulation.setParams(_params);
        times = new double[num];
        for (int i=0; i<num; i++)
        {
            cde = SymmetricDivisionEvent.generate(0.0, _cell);
            times[i] = cde.getTime();
        }
        testExponential(_cell.getDivisionRate(), floor, 
                        times, "CellDivFloor.txt");
	}

    
    public void testAsymmDivision() throws IOException
    {
        int num = 20000;
        double time = 0.0;

        double floor = 0.5;
        _params.setProperty("event.celldivision.floor", String.valueOf(floor));

        SymmetricDivisionEvent symm = SymmetricDivisionEvent.generate(time, _cell);
        AsymmetricDivisionEvent asymm = 
            AsymmetricDivisionEvent.generate(time, _cell);
        
        for (int i=0; i<num; i++)
        {
            while (asymm.getTime() > symm.getTime())
            {
                time = Math.max(asymm.getTime(), symm.getTime());
                symm = SymmetricDivisionEvent.generate(time, _cell);
                asymm = AsymmetricDivisionEvent.generate(time, _cell);
            }
            
            assertTrue(symm.getTime() - asymm.getTime() >= floor);
            
            time = Math.max(asymm.getTime(), symm.getTime());
            asymm = AsymmetricDivisionEvent.generate(0.0, _cell);
        }
	}
    
    
    
    public void testApoptosis() throws IOException
    {
        ApoptosisEvent cde = null;
        int num = 20000;
        double[] times = null;
        double floor = 0.0;
        
        // no floor
        times = new double[num];
        for (int i=0; i<num; i++)
        {
            cde = ApoptosisEvent.generate(0.0, _cell);
            times[i] = cde.getTime();
        }
        testExponential(_cell.getApoptosisRate(), 
                        floor, 
                        times, 
                        "Apop.txt");
        
        // with floor
        floor = 4.0;
        _params.setProperty("event.apoptosis.floor", "" + floor);
        _simulation.setParams(_params);
        times = new double[num];
        for (int i=0; i<num; i++)
        {
            cde = ApoptosisEvent.generate(0.0, _cell);
            times[i] = cde.getTime();
        }
        testExponential(_cell.getApoptosisRate(), 
                        floor, 
                        times, 
                        "ApopFloor.txt");
	}

}

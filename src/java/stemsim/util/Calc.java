package stemsim.util;

import java.util.*;
import java.util.regex.*;


public class Calc
{
    static public double mean(List<Double> $vals)
    {
        double sum = 0.0;
        int count = 0;
        for (double v : $vals)
        {
            sum += v;
            count ++;
        }
        
        return (double)sum/(double)count;
    }
    
    
    static public double stDev(List<Double> $vals)
    {
        double mean = mean($vals);
        double sumofsquares = 0.0;
        int count = 0;
        for (double v : $vals)
        {
            double d = (v) - mean;
            d = d * d;
            sumofsquares += d;
            count ++;
        }
        
        double variance = sumofsquares/(double)count;
        double stdev = Math.sqrt(variance);
        
        return stdev;
    }
    
    
    static public double stErr(List<Double> $vals)
    {
        double stdev = stDev($vals);
        double sterr = stdev/Math.sqrt($vals.size());
        
        return sterr;
    }
    
    

    /**
     * Compute the mean of a list of interegers normalized by a time value.
     * Intended to compute normalized means across simulation runs.
     *
     */
    static public double mean(List<Integer> $counts, double $time)
    {
        double sum = 0.0;
        int count = 0;
        for (int mc : $counts)
        {
            sum += mc;
            count ++;
        }
        sum /= $time;
        
        return (double)sum/(double)count;
    }

    
    /**
     * Compute the standard deviation of a list of integers normalized by time.
     * Intended to compute normalized standard deviations across simulation
     * runs.
     *
     */
    static public double stDev(List<Integer> $counts, double $time)
    {
        double mean = mean($counts, $time);
        double sumofsquares = 0.0;
        int count = 0;
        for (int mc : $counts)
        {
            double d = (mc/$time) - mean;
            d = d * d;
            sumofsquares += d;
            count ++;
        }
        
        double variance = sumofsquares/(double)count;
        double stdiv = Math.sqrt(variance);
        
        return stdiv;
    }
}

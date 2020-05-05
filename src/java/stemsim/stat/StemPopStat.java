package stemsim.stat;


import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import stemsim.event.*;
import stemsim.object.*;
import stemsim.simulation.*;


/**
 * StemPopStat monitors crypts and records their stem cell population size as a
 * function of time.
 *
 */
public class StemPopStat extends SimEventListenerAdapter
{
    static DecimalFormat dform = new DecimalFormat("#.##");
    static DecimalFormat lform = new DecimalFormat("#.####");
    
    /* the latest pop measurement for each crypt */
    Map<Crypt,PopMeasurement> _pops;
    
    /* the up-to-date average pop size for each crypt */
    Map<Crypt,AveragePop> _avgs;

    // interval in days to take measurements
    int _interval = -1;
    
    // base mut rate for comparison
    double _baseMutRate = -1.0;

    // file stuff
    File _outfile = null;
    PrintWriter _out = null;
    
    // print detail files
    boolean _detail = false;
    
    
    /**
     * Start of new simulation
     *
     */ 
    public void start(SimulationStartEvent $event) 
    {
        _pops = new HashMap<Crypt,PopMeasurement>();
        _avgs = new HashMap<Crypt,AveragePop>();
        
        Simulation sim = $event.getSubject();
        SimulationParams params = sim.getParams();
        
        _interval = params.getInt("stempopstat.interval");
        _baseMutRate = params.getDouble("stemcell.mutationrate.base");
        _detail = params.isTrue("stempopstat.detail");
        
        if (_detail)
        {
            String fname = "stempopstat";
            try
            {
                fname += sim.getId() + "_" + sim.getLabel() +  ".txt";
                _outfile = new File(sim.getOutputDirectory(), fname);
                
                FileWriter fw = new FileWriter(_outfile);
                _out = new PrintWriter(fw);
                
                _out.println("crypt\ttime\tpop\tdead neighbors\tmutator"
                             + "\tbifurcation\tavg apop\tavg div\t avg ben apop"
                             + "\tavg ben div\tavg del apop\tavg del div"
                             + "\tavg mutator\tcrypt feedback");
                
            }
            catch (IOException e)
            {
                System.err.println("could not init " + fname);
                e.printStackTrace();
            }
        }
    }
    
    
    /**
     * In the event of crypt bifurcation, record the birth time of the daughter
     * crypt.
     *
     */
    public void bifurcation(CryptBifurcationEvent $event)
    {
        //System.err.println("Bifurcation: " + $event.getTime());
        
        Crypt crypt = $event.getSubject();
        takeMeasurement(crypt, $event.getTime(), true);
    }

    
    public void division(SymmetricDivisionEvent $event)
    {
        StemCell subject = $event.getSubject();
        Crypt crypt = subject.getCrypt();
        
        takeMeasurement(crypt, $event.getTime(), false);
    }

    
    public void apoptosis(ApoptosisEvent $event)
    {
        Crypt crypt = $event.getCrypt();
        
        takeMeasurement(crypt, $event.getTime(), false);
    }

    
    void takeMeasurement(Crypt $crypt, double $time, boolean $bifurcation)
    {
        if ($crypt == null)
        {
            System.err.println("StemPopStat.takeMeasurement null crypt"
                               + " at time " + $time);
            return;
        }

        double lasttime = -_interval;

        PopMeasurement last = _pops.get($crypt);
        if (last != null)
        {
            lasttime = last._time;
        }
        
        if ($time - lasttime > _interval || $bifurcation)
        {
            measure($crypt, $time, $bifurcation);
        }
    }

    
    void measure(Crypt $crypt, double $time, boolean $bifurcation)
    {
        if ($crypt.getStemCells() == null)
        {
            System.err.println("StemPopStat.measure no stem cells in crypt: "
                               + $crypt);
            return;
        }
        
        PopMeasurement pm = new PopMeasurement();
        pm._time = $time;
        pm._popsize = $crypt.getStemCells().size();
        pm._deadNeighbors = $crypt.deadNeighbors().size();
        pm._bifurcation = $bifurcation;
        
        Set<StemCell> cells = $crypt.getStemCells();
        for (StemCell cell : cells)
        {
            if (cell.getMutationRate() > _baseMutRate)
            {
                pm._mutator = true;
                break;
            }
        }
        
        _pops.put($crypt, pm);

        AveragePop ap = _avgs.get($crypt);
        if (ap == null)
        {
            ap = new AveragePop();
            _avgs.put($crypt, ap);
        }
        ap.calculate(pm._popsize);
        
        // conditionally print detailed output
        if (_detail)
        {
            _out.print($crypt.getId());
            _out.print("\t");
            _out.print(dform.format(pm._time));
            _out.print("\t");
            _out.print(pm._popsize);
            _out.print("\t");
            _out.print(pm._deadNeighbors);
            _out.print("\t");
            
            if (pm._mutator) _out.print("1");
            else _out.print("0");
            _out.print("\t");
            
            if (pm._bifurcation) _out.print("1");
            else _out.print("0");
            
            Crypt.Stats stats = $crypt.getStats();
            for (double d : stats.vals)
            {
                _out.print("\t");
                _out.print(lform.format(d));
            }
            
            _out.print("\t");
            _out.print(lform.format($crypt.apoptosisEffect(1.0)));
            
            _out.println();
        }
    }
    
    
    /**
     * At the end of the simulation, write the output file.
     *
     */
    public void end(SimulationEndEvent $event)
    {
        System.err.println("END");

        double time = $event.getTime();
        Simulation sim = $event.getSubject();
        if (sim instanceof TissueSimulation)
        {
            TissueSimulation ts = (TissueSimulation)sim;
            Tissue t = ts.getTissue();
            
            Set<Crypt> allCrypts = new HashSet<Crypt>();
            for (int i=0; i<t.numRows(); i++)
            {
                allCrypts.addAll(t.getRow(i));
            }
            
            for (Crypt c : allCrypts)
            {
                measure(c, time, false);
            }
        }
        
        if (sim instanceof CryptSimulation)
        {
            CryptSimulation cs = (CryptSimulation)sim;
            Crypt c = cs.getCrypt();
            measure(c, time, false);
        }
        
        if (_detail)
        {
            _out.flush();
            _out.close();
        }
        
        avgFile($event.getSubject());
    }
    
    
    void avgFile(Simulation $sim)
    {
        SimulationParams params = $sim.getParams();
        
        File outfile = null;
        FileWriter fw = null;
        PrintWriter out = null;
        
        try
        {
            outfile = new File($sim.getOutputDirectory(), "stempopstat.txt");
            
            if (outfile.exists())
            {
                fw = new FileWriter(outfile, true);
                out = new PrintWriter(fw);
            }
            else
            {
                fw = new FileWriter(outfile);
                out = new PrintWriter(fw);
                out.println("sim\tcrypt\tpop\tnum");
            }
                
            double total = 0.0;
            int count = 0;
            
            for (Crypt c : _avgs.keySet())
            {
                AveragePop p = _avgs.get(c);
                total += p.avg;
                count++;

                out.print($sim.getLabel());
                out.print("\t");
                out.print(c.getId());
                out.print("\t");
                out.print(dform.format(p.avg));
                out.print("\t");
                out.print(p.num);
                out.println();
            }
            
            /*
            double avg = total / (double)count;
            out.println();
            out.print("grand average\t");
            out.print(dform.format(avg));
            out.println();
            */
            
            out.flush();
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("could not write: " + outfile);
            e.printStackTrace();
        }
    }
    
    
    
    class PopMeasurement
    {
        double _time;
        int _popsize;
        int _deadNeighbors = 0;
        boolean _mutator = false;
        boolean _bifurcation = false;
        
        public String toString()
        {
            return _time + "\t" + _popsize + " " + _deadNeighbors + "\t"
            + _mutator + "\t" + _bifurcation;
        }
    }
   

    class AveragePop
    {
        double avg = 0;
        long num = 0;
        
        public void calculate(int $size)
        {
            if (num == 0)
            {
                avg = $size;
                num = 1;
                return;
            }
            
            avg = ((num * avg) + $size) / (num + 1.0);
            num ++;
        }
    }
    
}
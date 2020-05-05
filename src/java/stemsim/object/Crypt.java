package stemsim.object;


import java.io.StringWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

import stemsim.event.*;
import stemsim.simulation.*;


/**
 * Represents a crypt containing some number of stem cells.
 *
 */
public class Crypt extends SimulationObject implements StemCellAffector
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Unique identifier for Crypt objects */
    static int ID = 1;
    static int nextId()
    {
        return ID++;
    }

    /** Formatter for text output */
    static DecimalFormat dform = new DecimalFormat("#.###");

    
    /**
     * Create an isolated Crypt with a number of stem cells taken from
     * the simulation parameters.
     *
     */
    public static Crypt create(Simulation $simulation)
    {
        SimulationParams params = $simulation.getParams();
        int numstemcells = params.getInt("crypt.numcells.mean");
        return create($simulation, numstemcells, 0);
    }

    
    /**
     * Create a Crypt that can have the given number of neighbors.
     *
     */
    public static Crypt create(Simulation $simulation, int $numNeighbors)
    {
        SimulationParams params = $simulation.getParams();
        int numstemcells = params.getInt("crypt.numcells.mean");
        return create($simulation, numstemcells, $numNeighbors);
    }
    
    
    /**
     * Create a crypt with the given number of neighbors and stem cells.
     *
     */
    public static Crypt create(Simulation $simulation, 
                               int $numStemCells,
                               int $numNeighbors)
    {
        Crypt crypt = new Crypt($simulation, $numNeighbors);
        for (int i=0; i<$numStemCells; i++)
        {
            StemCell cell = StemCell.create(crypt);
        }
        return crypt;
    }

    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////

    /** Unique identifier for this Crypt object */
    int _id = nextId();

    /** The tissue of which this crypt is a part */
    Tissue _tissue;
    
    /** The stem cells in this crypt */
    Set<StemCell> _stemCells = new HashSet<StemCell>();
    
    /** The neighbors of this crypt */
    Crypt[] _neighbors = null;

    /** Hex coordinates used to compute hex grid distance */
    HexPoint _hexPoint;

    
    /**
     * Constructor.
     *
     */
    public Crypt(Simulation $simulation, int $numNeighbors)
    {
        super($simulation);
        _neighbors = new Crypt[$numNeighbors];
    }
    
    
    /**
     * Assign this crypt to a tissue
     *
     */
    public void setTissue(Tissue $tissue)
    {
        _tissue = $tissue;
    }
    
    /**
     * Get the tissue where this crypt lives
     *
     */
    public Tissue getTissue()
    {
        return _tissue;
    }
    
    
    /**
     * Returns the set of stem cells in this crypt.
     *
     */
    public Set<StemCell> getStemCells()
    {
        return _stemCells;
    }
    
    
    /**
     * Return the ordered list of neighbors of this crypt.
     *
     */
    public Crypt[] getNeighbors()
    {
        return _neighbors;
    }
    
    
    public void setHexPoint(int $x, int $y)
    {
        _hexPoint = new HexPoint($x, $y);
    }
    
    
    public HexPoint getHexPoint()
    {
        return _hexPoint;
    }
    
    
    public int hexDistance(Crypt $c)
    {
        HexPoint a = getHexPoint();
        HexPoint b = $c.getHexPoint();
        
        return a.distance(b);
    }
    
    
    /**
     * Set the neighbors of this crypt... not expected to be called after
     * the simulation has started.
     *
     */
    public void setNeighbors(Crypt[] $crypts)
    {
        _neighbors = $crypts;
    }
    
    
    /**
     * Return the list of dead neighbors of this crypt.
     *
     */
    public List<Crypt> deadNeighbors()
    {
        Crypt[] neighbors = getNeighbors();
        List<Crypt> deadneighbors = new LinkedList<Crypt>();
        for (Crypt tc : neighbors)
        {
            if (tc != null && !tc.isAlive())
            {
                deadneighbors.add(tc);
            }
        }
        return deadneighbors;
    }
    
    
    public boolean hasDeadNeighbor()
    {
        for (Crypt tc : getNeighbors())
        {
            if (tc != null && !tc.isAlive())
            {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * Add a stem cell to this crypt.
     *
     */
    public void add(StemCell $cell)
    {
        _stemCells.add($cell);
        $cell.setCrypt(this);
    }
    
    
    /**
     * Remove a stem cell from this crypt.
     *
     */
    public boolean remove(Cell $cell)
    {
        if ($cell == null) return false;
        
        boolean removed = false;

        // We considered adding transient cells as well
        if ($cell instanceof StemCell)  
        {
            // this is where the cell gets removed from the list (container)
            removed = _stemCells.remove($cell); 
            if (removed)
            {
                $cell.setCrypt(null);
            }
        }

        // DEBUG
        if (!removed)
        {
            throw new RuntimeException("could not remove cell in " + getId());
        }
        // DEBUG
        
        return removed;
    }
    
    
    /**
     * Get the unique identifier of this crypt.
     *
     */
    public int getId()
    {
        return _id;
    }
    
    
    /**
     * Return true if this stem cell is alive -- if there is at least one stem
     * cell still living in the crypt.
     *
     */
    public boolean isAlive()
    {
        for (StemCell cell : getStemCells())
        {
            if (cell.isAlive()) return true;
        }
        
        return false;
    }
    
    
    /**
     * Return true if this crpt is cancerous -- if it's number of stem cells
     * exceeds the cancer threshold.
     *
     */
    public boolean isCancerous()
    {
        SimulationParams params = getSimulation().getParams();
        
        // cancer is defined as having a number of stemcells greater than
        // the equilibrium level times a threshold
        double stemmean = params.getDouble("crypt.numcells.mean");
        double cancerthresh = 
            params.getDouble("cancer.uncontrolledgrowth.threshold");
        cancerthresh *= stemmean;
        
        // Could be optimized by storing this value in the params at the start 
        // of a run
        long thresh = Math.round(cancerthresh); 
        
        return (getStemCells().size() > thresh);
    }
    
    
    /**
     * Return a text representation of this object.
     *
     */
    public String toString()
    {
        String nl = System.getProperty("line.separator");

        StringBuffer buf = new StringBuffer();
        buf.append("Crypt(");
        buf.append(_id);
        buf.append(" cells:");
        buf.append(getStemCells().size());
        buf.append(" apop(1):");
        buf.append(dform.format(apoptosisEffect(1.0)));
        buf.append(" div(1):");
        buf.append(dform.format(divisionEffect(1.0)));
        buf.append(")");
        buf.append(nl);
        
        for (StemCell cell : getStemCells())
        {
            buf.append("  ");
            buf.append(cell);
            buf.append(nl);
        }

        return buf.toString();
    }

    
    /**
     * Return an XML representation of this object.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<crypt>");

        pw.print("<id>");
        pw.print(getId());
        pw.println("</id>");
        
        pw.print("<alive>");
        pw.print(isAlive());
        pw.println("</alive>");
        
        if (isCancerous())
        {
            pw.print("<unstable>");
            pw.print("true");
            pw.println("</unstable>");
        }
        
        pw.println("<effects>");
        pw.println("<apoptosis>");
        pw.println(apoptosisEffect(1.0));
        pw.println("</apoptosis>");
        pw.println("<division>");
        pw.println(divisionEffect(1.0));
        pw.println("</division>");
        pw.println("<mutation>");
        pw.println(mutationEffect(1.0));
        pw.println("</mutation>");
        pw.println("</effects>");
        
        pw.println("<stemcells>");
        for (StemCell s : getStemCells())
        {
            pw.println(s.toXML());
        }
        pw.println("</stemcells>");

        pw.println("</crypt>");
        
        return sw.toString();
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // StemCellAffector Interface
    ////////////////////////////////////////////////////////////////////////////
    
    public boolean isAboveHomeostatic()
    {
        SimulationParams params = getSimulation().getParams();

        int numcells = getStemCells().size();
        int mean = params.getInt("crypt.numcells.mean");
        
        return numcells > mean;
    }
    
    
    /**
     * Return the apoptotic effect this crypt will have on its stem cells.
     *
     */
    public double apoptosisEffect(double $rate)
    {
        SimulationParams params = getSimulation().getParams();
        
        // if have a dead neighbor, remove apoptotic effect
        if (deadNeighbors().size() > 0)
        {
            return $rate;
        }
        
        // compute effect based on number of stem cells
        int numcells = getStemCells().size();
        int mean = params.getInt("crypt.numcells.mean");
        int stdev = params.getInt("crypt.numcells.standarddeviation");
        
        double multiplier = params.getDouble("crypt.apoptosis.multiplier");
        
        double distanceFromMean  = (numcells - mean)/stdev;
        if (distanceFromMean <= 0) 
        {
            return $rate;
        }
        
        multiplier = Math.pow(multiplier, Math.abs(distanceFromMean));
        
        // System.out.println("distanceFromMean = " + distanceFromMean);
        // System.out.println(numCells + " => " + multiplier);
        
        return $rate * multiplier;
    }
    
    
    /**
     * Return the division effect this crypt will have on its stem cells.
     *
     */
    public double divisionEffect(double $rate)
    {
        SimulationParams params = getSimulation().getParams();
        
        // first calculate based on number of stem cells in this crypt
        int numCells = getStemCells().size();
        int mean = params.getInt("crypt.numcells.mean");
        int stdiv = params.getInt("crypt.numcells.standarddeviation");
        
        double distanceFromMean  = (numCells - mean)/stdiv;
        int numDeadNeighbors = deadNeighbors().size();
        
        // apply low population multiplier
        if (distanceFromMean < 0)
        {
            double popMult = params.getDouble("crypt.division.multiplier");
            popMult = Math.pow(popMult, Math.abs(distanceFromMean));
            
            $rate *= popMult;
        }
        
        //  apply dead neighbor muliplier
        if (numDeadNeighbors > 0)
        {
            double dnMult = 
                params.getDouble("crypt.division.deadneighbor.multiplier"); 
            
            $rate *= dnMult;
        }
        
        return $rate;
    }
    
    
    /**
     * Return the mutator effect this crypt will have on its stem cells -- none.
     *
     */
    public double mutationEffect(double $rate)
    {
        return $rate;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Inner Classes
    ////////////////////////////////////////////////////////////////////////////
    public class Stats
    {
        final int numvals = 7;
        
        public double[] vals = new double[numvals];
        
        public double avgApopRate;
        public double avgDivRate;
        
        public double avgNumBenApop;
        public double avgNumBenDiv;
        public double avgNumDelApop;
        public double avgNumDelDiv;
        public double avgNumMutator;
        
        public int numCellsTSG;
        boolean feedback = false;
        
        double[] vals()
        {
            double[] v = new double[numvals];
            for (int i=0; i<vals.length; i++)
            {
                v[i] = vals[i];
            }
            
            return v;
        }
        
        public String toString()
        {
            String s = "stats[";
            for (double d : vals)
            {
                s += " " + d;
            }
            s += "]";
            return s;
        }
    }

    
    public Stats getStats()
    {
        int numstemcells = getStemCells().size();
        if (numstemcells == 0)
        {
            return new Stats();
        }
        
        double[] totals = new double[] {
            0, 0, 0, 0, 0, 0, 0
        };
        
        for (StemCell cell : getStemCells())
        {
            totals[0] += cell.getApoptosisRate();
            totals[1] += cell.getDivisionRate();
            
            for (Mutation m : cell.getMutations())
            {
                if (!(m instanceof SummaryMutation)) continue;
                
                SummaryMutation sm = (SummaryMutation)m;
                if (sm.getLabel().equals("benApopSum")) 
                    totals[2]+= sm.getCount();
                if (sm.getLabel().equals("benDivSum")) 
                    totals[3]+= sm.getCount();
                if (sm.getLabel().equals("delApopSum")) 
                    totals[4]+= sm.getCount();
                if (sm.getLabel().equals("delDivSum")) 
                    totals[5]+= sm.getCount();
                if (sm.getLabel().equals("mutatorSum")) 
                    totals[6]+= sm.getCount();
            }
        }
        
        for (int i=0; i<2; i++)
        {
            totals[i] /= numstemcells;
        }
        
        for (int i=2; i<7; i++)
        {
            double avg = totals[i] / numstemcells;
            totals[i] = Math.round(avg);
        }
     

        // create and return the Stats object
        Stats stats = new Stats();

        stats.vals = totals;
        
        stats.avgApopRate = totals[0];
        stats.avgDivRate = totals[1];
        
        stats.avgNumBenApop = totals[2];
        stats.avgNumBenDiv = totals[3];
        stats.avgNumDelApop = totals[4];
        stats.avgNumDelDiv = totals[5];
        stats.avgNumMutator = totals[6];

        for (StemCell cell : getStemCells())
        {
            if (cell.getTSGHits() > 0)
            {
                stats.numCellsTSG++;
            }
        }            
        
        for (StemCell cell : getStemCells())
        {
            if (cell.getFeedbackApoptosisEvent() != null)
            {
                stats.feedback = true;
                break;
            }
        }
            
        //System.err.println("stats = " + stats);
        
        return stats;
    }
    
    
}

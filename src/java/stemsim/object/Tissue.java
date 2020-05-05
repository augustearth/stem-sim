package stemsim.object;


import java.io.StringWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

import stemsim.event.*;
import stemsim.simulation.*;


/**

Represents a tissue of crypts.  Crypts exist in a hexagonal map with the points
of the hexagon pointing up and down and the flat edges on the left and right.
Crypts are numbered and oriented as follows:

 5/\0
4|  |1
 3\/2

So, for a crypt, the 0th neighbor is to the northeast, the 4th to the west, and
so on.

*/
public class Tissue extends SimulationObject
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** unique identifier for Tissue objects */
    static int ID = 1;
    static int nextId()
    {
        return ID++;
    }

    /** formatters for string output */
    static DecimalFormat dform = new DecimalFormat("#.###");
    static DecimalFormat intform = new DecimalFormat("00");

    
    /**
     * Create a Tissue object for a given simulation.
     *
     */
    public static Tissue create(Simulation $simulation)
    {
        SimulationParams params = $simulation.getParams();
        boolean wraparound = params.getBoolean("tissue.wraparound");

        Tissue tissue = createFlatTissue($simulation);
        
        if (wraparound)
        {
            wrapVerticalEdges(tissue);
        }

        if (!validate(tissue))
        {
            throw new RuntimeException("tissue not valid!");
        }
        
        for (Crypt c : tissue.getCrypts())
        {
            c.setTissue(tissue);
        }
        
        setHexPoints(tissue);
        System.err.println(tissue.debugHexPoints());
        System.err.println(tissue.debugHexDistances());
        
        createInitialEvents(tissue);
        
        return tissue;
    }
    
    
    /**
     * Create the initial events for this tissue -- the apoptosis and
     * division events for the stem cells.
     * 
     */
    static void createInitialEvents(Tissue $tissue)
    {
        for (int i=0; i<$tissue.getRows().size(); i++)
        {
            for (Crypt tc : $tissue.getRow(i))
            {
                for (StemCell cell : tc.getStemCells())
                {
                    ApoptosisEvent.generate(0.0, cell);
                    ApoptosisEvent.generateFeedback(0.0, cell);
                    SymmetricDivisionEvent.generate(0.0, cell);
                    AsymmetricDivisionEvent.generate(0.0, cell);
                }
            }
        }
        
    }
    
    
    /**
     * Create a flat tissue with all crypts linked, but with no wrapped edges.
     *
     */
    static Tissue createFlatTissue(Simulation $simulation)
    {
        SimulationParams params = $simulation.getParams();
        int numrows = params.getInt("tissue.rows");
        int numcols = params.getInt("tissue.cols");

        if (numrows < 1)
        {
            throw new IllegalArgumentException("numrows < 1");
        }
        if (numcols < 1)
        {
            throw new IllegalArgumentException("numcols < 1");
        }
        
        Crypt firstinrow = createCrypt($simulation);
        List<Crypt> rows = new ArrayList<Crypt>(numrows);
        Crypt crypt = null;
        boolean seswflip = true;
        
        for (int row=0; row < numrows; row++)
        {
            crypt = firstinrow;
            rows.add(crypt);
            
            for (int col=0; col < numcols - 1; col++)
            {
                crypt = createEast(crypt);
            }
            
            if (row+1 < numrows)
            {
                if (seswflip)
                {
                    firstinrow = createSouthEast(firstinrow);
                }
                else
                {
                    firstinrow = createSouthWest(firstinrow);
                }
                seswflip = !seswflip;
            }
        }
        
        return new Tissue($simulation, rows);
    }
    
    
    /**
     * Wrap the vertical edges of the tissue by setting the crypt neighbors
     * at the edges.
     *
     */
    static void wrapVerticalEdges(Tissue $tissue)
    {
        for (Crypt first : $tissue.getRows())
        {
            Crypt last = first;
            while (last.getNeighbors()[1] != null)
            {
                last = last.getNeighbors()[1];
            }
            
            Crypt tc = null;
            
            last.getNeighbors()[1] = first;
            first.getNeighbors()[4] = last;
            
            tc = last.getNeighbors()[0];
            if (tc != null)
            {
                first.getNeighbors()[5] = tc;
                tc.getNeighbors()[2] = first;
            }
            
            tc = last.getNeighbors()[2];
            if (tc != null)
            {
                first.getNeighbors()[3] = tc;
                tc.getNeighbors()[0] = first;
            }
        }
    }
    
    
    /**
     * Set the hex points of the crypts in this tissue.
     *
     */
    static void setHexPoints(Tissue $tissue)
    {
        int numrows = $tissue.numRows();
        
        // need to keep track of the row we're working on.  after two rows,
        // our x hex coord needs to be shifted left by 1.  this is due to
        // the way a hex grid stored as rows is shifted to a true hex
        // coordinate system
        int shiftc = 1;
        int basex = 0;
        
        for (int y=numrows-1; y>=0; y--)
        {
            // for no good reason beside this was just the way i did it,
            // the rows as returned by getRos(int) are indexed from top to
            // bottom.  so, if we want the y hex coordinate to look normal,
            // we count down y 2, 1, 0, ... and count up what we pass to
            // getRow() 0, 1, 2, ...
            List<Crypt> row = $tissue.getRow(numrows - y - 1);

            if (shiftc-- < 0)
            {
                shiftc = 0;
                basex--;
            }
            
            int x = basex;
            
            for (Crypt c : row)
            {
                c.setHexPoint(x++, y);
            }
        }
    }
    
    
    /**
     * Create a crypt.
     *
     */
    static Crypt createCrypt(Simulation $simulation)
    {
        Crypt c = Crypt.create($simulation, 6);

        return c;
    }
    
    
    /**
     * Create an east neighbor for the given crypt.
     *
     */
    static Crypt createEast(Crypt $crypt)
    {
        Crypt newcell = createCrypt($crypt.getSimulation());
        
        $crypt.getNeighbors()[1] = newcell;
        newcell.getNeighbors()[4] = $crypt;

        Crypt tc = null;

        tc = $crypt.getNeighbors()[0];
        if (tc != null)
        {
            tc.getNeighbors()[2] = newcell;
            newcell.getNeighbors()[5] = tc;
        }

        tc = $crypt.getNeighbors()[2];
        if (tc != null)
        {
            tc.getNeighbors()[0] = newcell;
            newcell.getNeighbors()[3] = tc;
        }
        
        return newcell;
    }
    
    
    /**
     * Create a southeast neighbor for the given crypt.
     *
     */
    static Crypt createSouthEast(Crypt $crypt)
    {
        Crypt newcell = createCrypt($crypt.getSimulation());
        
        $crypt.getNeighbors()[2] = newcell;
        newcell.getNeighbors()[5] = $crypt;
        
        Crypt tc = null;
        
        tc = $crypt.getNeighbors()[1];
        if (tc != null)
        {
            tc.getNeighbors()[3] = newcell;
            newcell.getNeighbors()[0] = tc;
        }
        
        tc = $crypt.getNeighbors()[3];
        if (tc != null)
        {
            tc.getNeighbors()[1] = newcell;
            newcell.getNeighbors()[4] = tc;
        }
        
        return newcell;
    }
    
    
    /**
     * Create a southwest neighbor for the given crypt.
     *
     */
    static Crypt createSouthWest(Crypt $crypt)
    {
        Crypt newcell = createCrypt($crypt.getSimulation());
        
        $crypt.getNeighbors()[3] = newcell;
        newcell.getNeighbors()[0] = $crypt;
        
        Crypt tc = null;
        
        tc = $crypt.getNeighbors()[4];
        if (tc != null)
        {
            tc.getNeighbors()[2] = newcell;
            newcell.getNeighbors()[5] = tc;
        }
        
        tc = $crypt.getNeighbors()[2];
        if (tc != null)
        {
            tc.getNeighbors()[4] = newcell;
            newcell.getNeighbors()[1] = tc;
        }
        
        return newcell;
    }
    
    
    /**
     * Validate the tissue by making sure all the neighbor pointers match up.
     *
     */
    static boolean validate(Tissue $tissue)
    {
        List<Crypt> rows = $tissue.getRows();
        
        for (int i=0; i<$tissue.getRows().size(); i++)
        {
            for (Crypt tc : $tissue.getRow(i))
            {
                if (!validate(tc)) return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Validate a crypt by making sure that all the crypts neighbors point
     * to it appropriately.
     *
     */
    static boolean validate(Crypt $crypt)
    {
        Crypt tc = null;
        
        if (!validate($crypt, 0, 3)) return false;
        if (!validate($crypt, 1, 4)) return false;
        if (!validate($crypt, 2, 5)) return false;
        if (!validate($crypt, 3, 0)) return false;
        if (!validate($crypt, 4, 1)) return false;
        if (!validate($crypt, 5, 2)) return false;
        
        return true;
    }
    
    
    /**
     * Validate a specific neighborhood relation.
     *
     */
    static boolean validate(Crypt $crypt,
                            int $cryptside,
                            int $neighborside)
    {
        Crypt tc = $crypt.getNeighbors()[$cryptside];
        if (tc != null)
        {
            if (tc.getNeighbors()[$neighborside] != $crypt)
            {
                return false;
            }
        }
        return true;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    /** The unique identifier of this tissue. */
    int _id = nextId();
    
    /** A list of the rows of crypts in this tissue */
    List<Crypt> _rows = null;
    
    /** Stem cell and TAC TSG hits recorded in this crypt */
    List<TSGHit> _tsgHits = new LinkedList<TSGHit>();

    /** TSGHit book-keeping */
    boolean _knockoutTAC = false;
    
    /** TSGHit book-keeping */
    boolean _knockoutStem = false;
    
    
    /** 
     * Constructor 
     *
     */
    Tissue(Simulation $simulation, List<Crypt> $rows)
    {
        super($simulation);
        _rows = $rows;
    }
    
    
    /**
     * Return a list of Crypts object that are the leftmost crypts in the
     * rows of this tissue.
     *
     */
    public List<Crypt> getRows()
    {
        return _rows;
    }
    
    
    public int numRows()
    {
        return _rows.size();
    }
    
    
    /**
     * Get a specific row of this tissue.
     *
     */
    public List<Crypt> getRow(int $row)
    {
        List<Crypt> row = new LinkedList<Crypt>();
        
        Crypt first = getRows().get($row);
        Crypt tc = first;
        do
        {
            row.add(tc);
            tc = tc.getNeighbors()[1];
        }
        while (tc != null && tc != first);
        
        return row;
    }
    
    
	/**
	 * Get all crypts in a list without any guarantee on order.
	 *
	 */
	public List<Crypt> getCrypts()
	{
		Simulation sim = getSimulation();
        SimulationParams params = sim.getParams();
        int numrows = params.getInt("tissue.rows");
        int numcols = params.getInt("tissue.cols");
		
		List<Crypt> crypts = new ArrayList<Crypt>(numrows * numcols);
		int numRows = numRows();
		for (int i=0; i<numRows; i++)
		{
			for (Crypt c : getRow(i))
			{
				crypts.add(c);
			}
		}
		
		return crypts;
	}
	
	
    /**
     * Get the unique identifier of this tissue.
     *
     */
    public int getId()
    {
        return _id;
    }
    
    
    /**
     * Return true if there is at least one living crypt.
     *
     */
    public boolean isAlive()
    {
        for (int i=0; i<getRows().size(); i++)
        {
            // This is equivalent to a foreach command to get every "tc" 
            // from the Row
            for (Crypt tc : getRow(i)) 
            {
                if (tc.isAlive()) return true;
            }
        }
        return false;
    }

    
    // This could be made more efficient by only storing when a second TSG hit 
    // happens in a TAC (just the first time) and in a stem cell (first time).
    public void recordTSGCell(StemCell $cell)
    {
        // only record if we've had at least 1 stem hit and haven't already
        // recorded a 2nd hit
        if ($cell.getTSGHits() < 2 || _knockoutStem) return;
        
        _tsgHits.add(new TSGHit($cell, TSGLocation.STEM));
        _knockoutStem = true;
    }
    
    
    public void recordTSGTAC(StemCell $cell)
    {
        // only record if we've had at least 1 stem hit and haven't already
        // recorded a 2nd hit
        if ($cell.getTSGHits() < 1 || _knockoutTAC) return;
        
        _tsgHits.add(new TSGHit($cell, TSGLocation.TAC));
        _knockoutTAC = true;
    }
    
    
    public List<TSGHit> getTSGHits()
    {
        return _tsgHits;
    }
    
    
    public String debugHexPoints()
    {
        String nl = System.getProperty("line.separator");
        
        StringBuffer buf = new StringBuffer();

        for (int i=0; i<getRows().size(); i++)
        {
            for (Crypt tc : getRow(i))
            {
                Crypt crypt = tc;
                
                buf.append(tc.getHexPoint());
                buf.append(" ");
            }
            buf.append(nl);
        }
        
        return buf.toString();
    }

    
    public String debugHexDistances()
    {
        String nl = System.getProperty("line.separator");
        
        StringBuffer buf = new StringBuffer();
        
        Crypt topleft = getRows().get(0);
        
        for (int i=0; i<getRows().size(); i++)
        {
            for (Crypt tc : getRow(i))
            {
                Crypt crypt = tc;
                
                buf.append(tc.hexDistance(topleft));
                buf.append(" ");
            }
            buf.append(nl);
        }
        
        return buf.toString();
    }
    
    
    /**
     * Return a String representation of this tissue.
     *
     */
    public String toString()
    {
        String nl = System.getProperty("line.separator");

        StringBuffer buf = new StringBuffer();
        buf.append("Tissue(");
        buf.append(_id);
        buf.append(")");
        buf.append(nl);

        for (int i=0; i<getRows().size(); i++)
        {
            for (Crypt tc : getRow(i))
            {
                Crypt crypt = tc;
                
                int numcells = crypt.getStemCells().size();
                if (numcells < 10) buf.append(" ");
                buf.append(numcells);
                
                buf.append(mutString(tc));
                
                buf.append(" ");
            }
            buf.append(nl);
        }
        
        List<Crypt> deadborder = new LinkedList<Crypt>();
        for (Crypt c : getCrypts())
        {
            if (c.hasDeadNeighbor())
            {
                deadborder.add(c);
            }
        }
        
        if (deadborder.size() > 0)
        {
            buf.append("deadborder = ");
            for (Crypt c : deadborder)
            {
                buf.append(c.getId());
                buf.append(" ");
            }
            buf.append(nl);
        }

        buf.append(nl);
        
        return buf.toString();
    }
    
    
    /**
     * Return an XML representation of this tissue.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<tissue>");
        
        pw.println("<tsghits>");
        for (TSGHit tsg : getTSGHits())
        {
            pw.println(tsg.toXML());
        }
        pw.println("</tsghits>");
        
        for (int i=0; i<getRows().size(); i++)
        {
            pw.println("<row>");
            for (Crypt tc : getRow(i))
            {
                pw.println(tc.toXML());
            }
            pw.println("</row>");
        }
        
        pw.println("</tissue>");
        
        return sw.toString();
    }
 
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Utility
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Uses in the Tissue.toString().  Displays average rates and mutation
     * counts in the stem cells in a crypt.
     *
     * average apoptosis rate * 10
     * average division rate * 10
     * |
     * average # of beneficial apoptosis mutations
     * average # of beneficial division mutations
     * average # of deleterous apop mutations
     * average # of deleterous div mutations
     * average # of mutators mutations
     *
     * number of cells with at least one TSG knockout
     *
     */
    public String mutString(Crypt $crypt)
    {
        Crypt.Stats stats = $crypt.getStats();
        
        StringBuffer buf = new StringBuffer();

        if (stats.feedback) buf.append("[");
        else buf.append("(");

        double[] totals = stats.vals();
        
        int numstemcells = $crypt.getStemCells().size();
        for (int i=0; i<2; i++)
        {
            totals[i] *= 10.0;
        }
        
        for (int i=0; i<7; i++)
        {
            long n = Math.round(totals[i]);
            
            if (n == 0)
            {
                buf.append(" ");
            }
            else if (n <= 9)
            {
                buf.append(n);
            }
            else if (n <= 35)
            {
                buf.append(Character.valueOf((char)(n - 10 + 97)));
            }
            else if (n <= 61)
            {
                buf.append(Character.valueOf((char)(n - 36 + 65)));
            }
            else
            {
                buf.append("+");
            }
            
            if (i == 1)
            {
                buf.append("|");
            }
        }
        
        buf.append("|");
        buf.append(stats.numCellsTSG);
        
        if (stats.feedback) buf.append("]");
        else buf.append(")");

        return buf.toString();
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Inner Classes
    ////////////////////////////////////////////////////////////////////////////
    public enum TSGLocation 
    {
        TAC, STEM
    }
    
    public class TSGHit
    {
        StemCell _cell = null;
        TSGLocation _location;
        double _time = 0.0;
        
        public TSGHit(StemCell $cell, TSGLocation $loc)
        {
            _location = $loc;
            _time = $cell.getSimulation().getCurrentEvent().getTime();
            
            try
            {
                _cell = (StemCell)$cell.clone();
            }
            catch (CloneNotSupportedException e)
            {
                throw new RuntimeException(e);
            }
        }
        
        public String toXML()
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            
            pw.println("<tsghit>");
            
            pw.print("<time>");
            pw.print(_time);
            pw.println("</time>");
            
            pw.print("<crypt>");
            pw.print(_cell.getCrypt().getId());
            pw.println("</crypt>");
            
            pw.print("<location>");
            switch (_location)
            {
                case STEM:
                    pw.print("stemcell");
                    break;
                case TAC:
                    pw.print("tac");
                    break;
            }
            pw.println("</location>");

            pw.println(_cell.toXML());
            
            pw.println("</tsghit>");
            
            return sw.toString();
        }
    }
    
}

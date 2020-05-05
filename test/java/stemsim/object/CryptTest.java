package stemsim.object;


import java.io.*;
import java.util.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import stemsim.simulation.*;
import stemsim.object.*;
import stemsim.event.*;


public class CryptTest extends TestCase 
{
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    SimulationParams _params;
    TissueSimulation _simulation;

	protected void setUp() 
    {
        _params = new SimulationParams();

        _params.setProperty("simulation.type", "tissue");
        _params.setProperty("cryptstat.enabled", "false");
        _params.setProperty("stempopstat.enabled", "false");
        _params.setProperty("tissuepopstat.enabled", "false");
        _params.setProperty("divstat.enabled", "false");

        _params.setProperty("tissue.rows", "1");
        _params.setProperty("tissue.cols", "2");
        _params.setProperty("tissue.wraparound", "false");
        
        _params.setProperty("crypt.numcells.mean", "2");
        _params.setProperty("crypt.numcells.standarddeviation", "1");
        _params.setProperty("crypt.bifurcation.threshold.ratio", "2.0");

        _params.setProperty("stemcell.tacsize.base", "100");

        _params.setProperty("stemcell.divisionrate.base", "0.05");
        _params.setProperty("stemcell.asymmetricdivision.ratio", "20");
        _params.setProperty("crypt.division.multiplier", "2");
        _params.setProperty("crypt.division.deadneighbor.multiplier", "2");
        _params.setProperty("event.celldivision.floor", "0");

        _params.setProperty("stemcell.apoptosisrate.base", "0.05");
        _params.setProperty("crypt.apoptosis.multiplier", "2");
        _params.setProperty("event.apoptosis.floor", "0");
        
        _params.setProperty("stemcell.mutationrate.base", "0.0");
        _params.setProperty("stemcell.mutationrate.max", "0.0");

        _params.setProperty("crypt.transientcompartment.size", "10");

        _params.setProperty("cancer.uncontrolledgrowth.threshold", "4");
        _params.setProperty("cancer.tsg.threshold", "2");

        _simulation  = new TissueSimulation();
        _simulation.setParams(_params);
        _simulation.init(_params);
	}
    

	public void testApoptosisEffect() throws IOException
    {
        Tissue tissue = _simulation.getTissue();
        System.err.println(tissue);
        
        List<Crypt> crypts = tissue.getRow(0);
        Crypt c0 = crypts.get(0);
        Crypt c1 = crypts.get(1);
        System.err.println(c0);
        System.err.println(c1);
        
        // baseline
        assertEquals(1.0, c0.apoptosisEffect(1.0));
        assertEquals(1.0, c1.apoptosisEffect(1.0));
        
        // crypt control
        StemCell c = StemCell.create(c0);
        assertEquals(2.0, c0.apoptosisEffect(1.0));
        
        c.kill();
        c0.remove(c);
        assertEquals(1.0, c0.apoptosisEffect(1.0));

        // dead neighbor
        c = StemCell.create(c0);
        assertEquals(2.0, c0.apoptosisEffect(1.0));
        
        List<StemCell> cells = new LinkedList<StemCell>();
        cells.addAll(c1.getStemCells());
        for (StemCell cell : cells)
        {
            cell.kill();
            c1.remove(cell);
        }
        assertFalse(c1.isAlive());
        
        assertEquals(1.0, c0.apoptosisEffect(1.0));
	}

    
    public void testDivisionEffect() throws IOException
    {
        Tissue tissue = _simulation.getTissue();
        System.err.println(tissue);
        
        List<Crypt> crypts = tissue.getRow(0);
        Crypt c0 = crypts.get(0);
        Crypt c1 = crypts.get(1);
        System.err.println(c0);
        System.err.println(c1);
        
        // baseline
        assertEquals(1.0, c0.divisionEffect(1.0));
        assertEquals(1.0, c1.divisionEffect(1.0));
        
        // crypt control
        StemCell c = StemCell.create(c0);
        assertEquals(1.0, c0.divisionEffect(1.0));
        
        c.kill();
        c0.remove(c);
        assertEquals(1.0, c0.divisionEffect(1.0));
        
        List<StemCell> cells = new LinkedList<StemCell>();
        cells.addAll(c0.getStemCells());

        c = cells.get(0);
        c.kill();
        c0.remove(c);
        assertEquals(2.0, c0.divisionEffect(1.0));

        c = StemCell.create(c0);
        assertEquals(1.0, c0.divisionEffect(1.0));
        
        // dead neighbor
        cells = new LinkedList<StemCell>();
        cells.addAll(c1.getStemCells());
        for (StemCell cell : cells)
        {
            cell.kill();
            c1.remove(cell);
        }
        assertFalse(c1.isAlive());
        
        assertEquals(2.0, c0.divisionEffect(1.0));
	}
    
    
    public void testCryptBifurcation() throws IOException
    {
        Crypt c0 = Crypt.create(_simulation, 2, 1);
        Crypt c1 = Crypt.create(_simulation, 2, 1);
        
        c0.setNeighbors(new Crypt[]{c1});
        c1.setNeighbors(new Crypt[]{c0});

        assertEquals(0, c0.deadNeighbors().size());
        assertEquals(0, c1.deadNeighbors().size());

        List<StemCell> cells = new LinkedList<StemCell>();
        cells.addAll(c0.getStemCells());
        for (StemCell cell : cells)
        {
            cell.kill();
            c0.remove(cell);
        }
        
        // we killed c0, so c1 has one dead neighbor
        assertEquals(1, c1.deadNeighbors().size());
        
        // CryptChangeEvent shouldn't do anything as we haven't reached the
        // bifurcation threshold
        CryptChangeEvent.run(c1, 0.0);
        EventQueue q = _simulation.getEventQueue();
        SimulationEvent e = q.peek();
        assertFalse(e instanceof CryptBifurcationEvent);
        
        // add a single stem cell, still not at threshold
        StemCell c = StemCell.create(c1);
        CryptChangeEvent.run(c1, 0.0);
        e = q.peek();
        assertFalse(e instanceof CryptBifurcationEvent);

        // add another stem cell and we SHOULD be at threshold
        c = StemCell.create(c1);
        CryptChangeEvent.run(c1, 0.0);
        e = q.peek();
        assertTrue(e instanceof CryptBifurcationEvent);
        
        // run the actual bifurcation event
        e.unfold();
        assertEquals(0, c1.deadNeighbors().size());
        assertEquals(2, c0.getStemCells().size());
        assertEquals(2, c1.getStemCells().size());
        
	}
    
}
package stemsim.object;


import java.io.*;
import java.util.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import stemsim.simulation.*;
import stemsim.object.*;
import stemsim.event.*;


public class StemCellTest extends TestCase 
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
        
        _params.setProperty("crypt.numcells.mean", "1");
        _params.setProperty("crypt.numcells.standarddeviation", "1");

        _params.setProperty("stemcell.tacsize.base", "100");

        _params.setProperty("stemcell.divisionrate.base", "0.05");
        _params.setProperty("stemcell.asymmetricdivision.ratio", "20");
        _params.setProperty("crypt.division.multiplier", "1");
        _params.setProperty("crypt.division.deadneighbor.multiplier", "1");
        _params.setProperty("event.celldivision.floor", "0");

        _params.setProperty("stemcell.apoptosisrate.base", "0.05");
        _params.setProperty("crypt.apoptosis.multiplier", "2");
        _params.setProperty("event.apoptosis.floor", "0");
        
        _params.setProperty("stemcell.mutationrate.base", "0.0005");
        _params.setProperty("stemcell.mutationrate.max", "0.05");

        _params.setProperty("beneficialmutation.apoptosis.multiplier", "0.5");
        _params.setProperty("beneficialmutation.division.multiplier", "2.0");
        _params.setProperty("deleteriousmutation.apoptosis.multiplier", "4.0");
        _params.setProperty("deleteriousmutation.division.multiplier", "0.25");
        _params.setProperty("mutatormutation.mutation.multiplier", "100");

        _simulation  = new TissueSimulation();
        _simulation.setParams(_params);
        _simulation.init(_params);
	}
    
    
    public void testCorrectDivEvents()
    {
        Tissue tissue = _simulation.getTissue();
        System.err.println(tissue);
        
        List<Crypt> crypts = tissue.getRow(0);
        Crypt c = crypts.get(0);
        Set<StemCell> stemCells = c.getStemCells();
        StemCell[] sa = stemCells.toArray(new StemCell[0]); 
        StemCell s = sa[0];
        boolean r = false;
        double stime = 0.0;
        double atime = 0.0;
        
        System.err.println(s);
        SymmetricDivisionEvent sdiv = s.getDivisionEvent();
        AsymmetricDivisionEvent adiv = s.getAsymmetricDivisionEvent();

        // test do nothing cases
        atime = adiv.getTime();
        stime = sdiv.getTime();

        s.setAsymmetricDivisionEvent(null);
        r = s.correctDivEvents();
        assertFalse(r);
        assertEquals(atime, adiv.getTime());
        assertEquals(stime, sdiv.getTime());

        s.setAsymmetricDivisionEvent(adiv);
        s.setDivisionEvent(null);
        r = s.correctDivEvents();
        assertFalse(r);
        assertEquals(atime, adiv.getTime());
        assertEquals(stime, sdiv.getTime());

        s.setDivisionEvent(sdiv);
        s.setAsymmetricDivisionEvent(adiv);

        double floor = 0.5;
        _params.setProperty("event.celldivision.floor", String.valueOf(floor));
        sdiv.setTime(atime + floor + (0.01 * floor));
        stime = sdiv.getTime();
        r = s.correctDivEvents();
        assertFalse(r);
        assertEquals(atime, adiv.getTime());
        assertEquals(stime, sdiv.getTime());
        
        // test do something cases
        sdiv.setTime(atime + (0.1 * floor));
        stime = sdiv.getTime();
        r = s.correctDivEvents();
        assertTrue(r);
        assertEquals(atime, adiv.getTime());
        assertEquals(sdiv.getTime(), atime + floor);
    }
    

	public void testApoptosisRate() throws IOException
    {
        Tissue tissue = _simulation.getTissue();
        System.err.println(tissue);
        
        List<Crypt> crypts = tissue.getRow(0);
        Crypt c = crypts.get(0);
        Set<StemCell> stemCells = c.getStemCells();
        StemCell[] sa = stemCells.toArray(new StemCell[0]); 
        StemCell s = sa[0];
        
        System.err.println(s);
        
        // baseline
        assertEquals(0.05, s.getApoptosisRate());
        assertEquals(0.05, s.getDivisionRate());
        
        // ben apop
        Mutation m = BeneficialApopMutation.create(_simulation);
        s.addMutation(m);
        assertEquals(0.025, s.getApoptosisRate());
        assertEquals(0.05, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());

        s.addMutation(m);
        assertEquals(0.0125, s.getApoptosisRate());
        assertEquals(0.05, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());
        
        // ben div
        m = BeneficialDivMutation.create(_simulation);
        s.addMutation(m);
        assertEquals(0.0125, s.getApoptosisRate());
        assertEquals(0.1, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());
        
        s.addMutation(m);
        assertEquals(0.0125, s.getApoptosisRate());
        assertEquals(0.2, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());

        // del apop
        m = DeleteriousApopMutation.create(_simulation);
        s.addMutation(m);
        assertEquals(0.05, s.getApoptosisRate());
        assertEquals(0.2, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());
        
        s.addMutation(m);
        assertEquals(0.2, s.getApoptosisRate());
        assertEquals(0.2, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());

        // del div
        m = DeleteriousDivMutation.create(_simulation);
        s.addMutation(m);
        assertEquals(0.2, s.getApoptosisRate());
        assertEquals(0.05, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());
        
        s.addMutation(m);
        assertEquals(0.2, s.getApoptosisRate());
        assertEquals(0.0125, s.getDivisionRate());
        assertEquals(0.0005, s.getMutationRate());
        
        // mutator
        m = MutatorMutation.create(_simulation);
        s.addMutation(m);
        assertEquals(0.2, s.getApoptosisRate());
        assertEquals(0.0125, s.getDivisionRate());
        assertEquals(0.05, s.getMutationRate());
        
        // TRICKY: mutation rate is capped at max, so no change
        s.addMutation(m);
        assertEquals(0.2, s.getApoptosisRate());
        assertEquals(0.0125, s.getDivisionRate());
        assertEquals(0.05, s.getMutationRate());
        
	}

}

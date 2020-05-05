package stemsim.object;


import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.*;
import java.text.DecimalFormat;

import stemsim.event.*;
import stemsim.simulation.*;


/**
 * Superclass for a cell the lives inside a crypt.  StemCell is a sub-class of
 * Cell.
 *
 */
public class Cell extends SimulationObject implements Cloneable
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Static ID useful in toString()
     *
     */
    static int CELL_ID = 1;
    static int nextId()
    {
        return CELL_ID++;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    /** int id useful in toString() */
    int _id = nextId();
    public int getId()
    {
        return _id;
    }
    
    /** the crypt where we live */
    Crypt _crypt = null;
    
    /** alive/dead flag */
    boolean _alive = true;
    
    /** our next scheduled apoptosis event */
    ApoptosisEvent _apoptosisEvent;
    
    /** our next scheduled division event */
    SymmetricDivisionEvent _cellDivisionEvent;
    
    /** 
     * List of individual mutation objects acquired by this cell.  Currently,
     * we do not store mutations here, only in the SummaryMutation objects
     * below.  This list of individual objects could be used to store
     * heterogenous-effect mutations.
     */
    List<Mutation> _mutations = new LinkedList<Mutation>();
    
    /** Deleterious apoptosis mutation summarizer */
    SummaryMutation _delApopSum;
    
    /** Deleterious division mutation summarizer */
    SummaryMutation _delDivSum;
    
    /** Beneficial apoptosis mutation summarizer */
    SummaryMutation _benApopSum;
    
    /** Beneficial division mutation summarizer */
    SummaryMutation _benDivSum;

    /** Mutator mutation summarizer */
    SummaryMutation _mutatorSum;
    
    
    /**
     * Private constructor.  Create cells using static create() method.
     *
     */
    protected Cell(Simulation $sim)
    {
        super($sim);
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Getter methods for the various rates associated with a cell.
    ////////////////////////////////////////////////////////////////////////////

    public double getApoptosisRate()
    {
        SimulationParams params = getSimulation().getParams();
        
        // start with base rate
        double rate = params.getDouble("stemcell.apoptosisrate.base");
        
        // factor non-standard mutations
        for (Mutation mut : _mutations)
        {
            rate = mut.apoptosisEffect(rate);
        }
        
        // factor appropriate summary mutation effects
        rate = _delApopSum.apoptosisEffect(rate);
        rate = _benApopSum.apoptosisEffect(rate);

        return rate;
    }
    
    
    public double getDivisionRate()
    {
        SimulationParams params = getSimulation().getParams();
        
        // start with base rate
        double rate = params.getDouble("stemcell.divisionrate.base");
        
        // factor crypt effect
        rate = getCrypt().divisionEffect(rate);
        
        // factor individual mutation object effects
        // in the current implementation, this list is empty
        for (Mutation mut : _mutations)
        {
            rate = mut.divisionEffect(rate);
        }

        // factor appropriate division mutation effects
        rate = _benDivSum.divisionEffect(rate);
        rate = _delDivSum.divisionEffect(rate);
        
        return rate;
    }

    
    public double getMutationRate()
    {
        SimulationParams params = getSimulation().getParams();
        double base = params.getDouble("stemcell.mutationrate.base");
        double max = params.getDouble("stemcell.mutationrate.max");
        return getMutationRate(base, max);
    }

    
    public double getTSGMutationRate()
    {
        SimulationParams params = getSimulation().getParams();
        double base = params.getDouble("stemcell.tsgmutationrate.base");
        double max = params.getDouble("stemcell.tsgmutationrate.max");
        return getMutationRate(base, max);
    }
    
    
    double getMutationRate(double $baserate, double $max)
    {
        SimulationParams params = getSimulation().getParams();
        
        // start with base rate
        double rate = $baserate;
        
        // factor crypt effect (which there is none, at the moment)
        rate = getCrypt().mutationEffect(rate);
        
        // factor non-standard mutations
        for (Mutation mut : _mutations)
        {
            rate = mut.mutationEffect(rate);
        }

        // factor mutator mutation summary effect
        rate = _mutatorSum.mutationEffect(rate);
        
        // cap at max mutation rate
        return Math.min(rate, $max);
    }
    
    
    /**
     * Get/Set the Crypt to which we belong.
     *
     */
    public void setCrypt(Crypt $crypt)
    {
        _crypt = $crypt;
    }
    
    public Crypt getCrypt()
    {
        return _crypt;
    }
    
    
    /**
     * Add a mutation to this cell.  
     *
     * If it's one of the mutations we summarize, then summarize it.  
     * Otherwise, add it to the list.
     *
     */
    public void addMutation(Mutation $mut)
    {
        if ($mut instanceof DeleteriousApopMutation)
        {
            _delApopSum.summarize($mut);
        }
        else if ($mut instanceof DeleteriousDivMutation)
        {
            _delDivSum.summarize($mut);
        }
        else if ($mut instanceof BeneficialApopMutation)
        {
            _benApopSum.summarize($mut);
        }
        else if ($mut instanceof BeneficialDivMutation)
        {
            _benDivSum.summarize($mut);
        }
        else if ($mut instanceof MutatorMutation)
        {
            _mutatorSum.summarize($mut);
        }
        else
        {
            _mutations.add($mut);
        }
    }
    
    
    /**
     * Convenience method: return true if this cell has a mutator mutation.
     *
     */
    public boolean isMutator()
    {
        if (_mutatorSum.getCount() > 0) return true;
        
        for (Mutation mut : _mutations)
        {
            if (mut instanceof MutatorMutation) return true;
        }
        
        return false;
    }
    
    
    /**
     * Return a list of all the mutations affecting this cell.
     *
     */
    public List<Mutation> getMutations()
    {
        List<Mutation> l = new ArrayList(_mutations.size() + 4);

        l.add(_delApopSum);
        l.add(_delDivSum);
        l.add(_benApopSum);
        l.add(_benDivSum);
        l.add(_mutatorSum);
        l.addAll(_mutations);
        
        return l;
    }

    
    /**
     * Get/Set the events associated with this cell.
     *
     */
    public void setApoptosisEvent(ApoptosisEvent $event)
    {
        _apoptosisEvent = $event;
    }
    
    public ApoptosisEvent getApoptosisEvent()
    {
        return _apoptosisEvent;
    }
    
    public void setDivisionEvent(SymmetricDivisionEvent $event)
    {
        _cellDivisionEvent = $event;
    }
    
    public SymmetricDivisionEvent getDivisionEvent()
    {
        return _cellDivisionEvent;
    }
    
    public void clearEvents()
    {
        EventQueue queue = getSimulation().getEventQueue();

        if (_apoptosisEvent != null)
        {
            _apoptosisEvent.invalidate();
            queue.remove(_apoptosisEvent);
            _apoptosisEvent = null;
        }

        if (_cellDivisionEvent != null)
        {
            _cellDivisionEvent.invalidate();
            queue.remove(_cellDivisionEvent);
            _cellDivisionEvent = null;
        }
    }
    
    /**
     * Kill this cell.
     *
     */
    public void kill()
    {
        _alive = false;
    }
    
    /**
     * Getter for alive bit.
     *
     */
    public boolean isAlive()
    {
        return _alive;
    }
    

    /**
     * Return a copy of this cell.
     *
     */
    public Object clone() throws CloneNotSupportedException
    {
        Cell clone =  (Cell)super.clone();
        
        // New ID for the daughter clone
        clone._id = nextId(); 
        
        clone._delApopSum = (SummaryMutation)_delApopSum.clone();
        clone._delDivSum = (SummaryMutation)_delDivSum.clone();
        clone._benApopSum = (SummaryMutation)_benApopSum.clone();
        clone._benDivSum = (SummaryMutation)_benDivSum.clone();
        clone._mutatorSum = (SummaryMutation)_mutatorSum.clone();
        
        return clone;
    }
    
}

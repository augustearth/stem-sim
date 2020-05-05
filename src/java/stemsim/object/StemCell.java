package stemsim.object;


import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.*;
import java.text.DecimalFormat;

import stemsim.event.*;
import stemsim.simulation.*;


/**
 * Represents a stem cell and its transient cell progeny in a crypt.
 *
 */
public class StemCell extends Cell implements Cloneable
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Method to create a StemCell object inside a given crypt.
     *
     */
    public static StemCell create(Crypt $crypt)
    {
        StemCell cell = new StemCell($crypt);
        $crypt.add(cell);
        
        return cell;
    }

    
    /**
     * Some static formatter objects useful in toString()
     *
     */
    static DecimalFormat dform = new DecimalFormat("#.###");
    static DecimalFormat tform = new DecimalFormat("#.#");
    static DecimalFormat sform = new DecimalFormat("#.##E0");
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** our next scheduled feedback-influenced apoptosis event */
    ApoptosisEvent _feedbackApoptosisEvent;
    
    /** our next scheduled asymmetric division event */
    AsymmetricDivisionEvent _asymmetricDivisionEvent;

    /** mutation hits to a tumor suppressor gene in this stem cell */
    int _tsgHits = 0;
    
    /** the TSG knockout mutations in the related TAC cells */
    int _tsgHitsTAC = 0;
    
    
    //
    // Getters and Setters
    //

    public void setFeedbackApoptosisEvent(ApoptosisEvent $event)
    {
        _feedbackApoptosisEvent = $event;
    }
    
    public ApoptosisEvent getFeedbackApoptosisEvent()
    {
        return _feedbackApoptosisEvent;
    }
    
    public void setAsymmetricDivisionEvent(AsymmetricDivisionEvent $event)
    {
        _asymmetricDivisionEvent = $event;
    }
    
    public AsymmetricDivisionEvent getAsymmetricDivisionEvent()
    {
        return _asymmetricDivisionEvent;
    }
    
    
    /**
     * Return the number of transient amplifying cells associated with this stem
     * cell.  There is a fixed number of stem cells in a crypt of which each
     * stem cell has an equal share.
     *
     */
    public double getTransientCells()
    {
        SimulationParams params = getSimulation().getParams();

        int tacSize = params.getInt("stemcell.tacsize.base");
        int numCells = getCrypt().getStemCells().size();

        return tacSize / numCells;
    }
    
    
    public int getTSGHits()
    {
        return _tsgHits;
    }
    
    public void tsgHit()
    {
        _tsgHits++;
    }

    public int getTSGHitsTAC()
    {
        return _tsgHitsTAC;
    }
    
    public void tsgHitTAC()
    {
        _tsgHitsTAC++;
    }

    public void tsgHitTACRemove()
    {
        _tsgHitsTAC--;
    }


    //
    // Event rates
    //
    public double getFeedbackApoptosisRate()
    {
        SimulationParams params = getSimulation().getParams();

        // start with base rate
        double rate = params.getDouble("stemcell.apoptosisrate.base");

        // factor crypt effect
        rate = getCrypt().apoptosisEffect(rate);
       
       return rate;
    }

    
    public double getAsymmetricDivisionRate()
    {
        SimulationParams params = getSimulation().getParams();

        // start with base rate
        double rate = params.getDouble("stemcell.divisionrate.base");

        // factor non-standard mutation effects
        for (Mutation mut : _mutations)
        {
            rate = mut.divisionEffect(rate);
        }

        // factor appropriate division mutation effects
        rate = _benDivSum.divisionEffect(rate);
        rate = _delDivSum.divisionEffect(rate);

        // multiply by asymmetric/symmetric ratio
        int asymmratio = params.getInt("stemcell.asymmetricdivision.ratio");
        rate *= (double)asymmratio;

        return rate;
    }
    
    
    /**
     * Return true if the number of TSG knockouts in the stem cel only 
     * exceeds the threshold.
     *
     */
    public boolean isCancerous()
    {
        SimulationParams params = getSimulation().getParams();

        int tsgThresh = params.getInt("cancer.tsg.threshold");
        return (getTSGHits() >= tsgThresh);
    }
    
    
    /**
     * If the symmetric division event is scheduled less than div floor time
     * after the asymm div, push the symm div event back.
     *
     * Returns true if we had to do a push.
     *
     */
    public boolean correctDivEvents()
    {
        SymmetricDivisionEvent sdiv = getDivisionEvent();
        if (sdiv == null) return false;
        
        AsymmetricDivisionEvent adiv = getAsymmetricDivisionEvent();
        if (adiv == null) return false;
        
        Simulation sim = getSimulation();
        SimulationParams params = sim.getParams();
        double floor = params.getDouble("event.celldivision.floor");
        
        double stime = sdiv.getTime();
        double atime = adiv.getTime();
        
        if (stime > atime && (stime - atime) < floor)
        {
            EventQueue queue = sim.getEventQueue();

            stime = atime + floor;
            queue.remove(sdiv);
            sdiv.setTime(stime);
            queue.offer(sdiv);

            return true;
        }
        
        return false;
    }
    
    
    
    /**
     * Private constructor.  Create stem cells using static create() method.
     *
     */
    private StemCell(Crypt $crypt)
    {
        // assign ourselves to the given crypt
        super($crypt.getSimulation());
        setCrypt($crypt);
        
        Simulation sim = $crypt.getSimulation();
        SimulationParams params = sim.getParams();

        // create baseline summary mutations
        _delApopSum = new SummaryMutation(sim, "delApopSum");
        _delDivSum = new SummaryMutation(sim, "delDivSum");
        _benApopSum = new SummaryMutation(sim, "benApopSum");
        _benDivSum = new SummaryMutation(sim, "benDivSum");
        _mutatorSum = new SummaryMutation(sim, "mutatorSum");
    }
    
    
    public void clearEvents()
    {
        super.clearEvents();
        
        EventQueue queue = getSimulation().getEventQueue();
        
        // We have separate rates of background apoptosis (handled by the super)
        // and the apoptosis due to homeostasis.
        // Mutations can affect the first but not the second.
        if (_feedbackApoptosisEvent != null) 
        {
            _feedbackApoptosisEvent.invalidate();
            queue.remove(_feedbackApoptosisEvent);
            _feedbackApoptosisEvent = null;
        }
        
        if (_asymmetricDivisionEvent != null)
        {
            _asymmetricDivisionEvent.invalidate();
            queue.remove(_asymmetricDivisionEvent);
            _asymmetricDivisionEvent = null;
        }
    }
    
    
    /**
     * Only used for debugging...
     *
     */
    public boolean isDoomed()
    {
        SimulationEvent div = getDivisionEvent();
        SimulationEvent apop = getApoptosisEvent();
        SimulationEvent apopf = getFeedbackApoptosisEvent();
        
        double divt = div.getTime();
        double apopt = apop.getTime();
        
        double apopft = Double.MAX_VALUE;
        if (apopf !=null) apopft = apopf.getTime();
        
        if (apopft < divt && apopft != divt)
        {
            return true;
        }

        if (apopt < divt && apopt != divt)
        {
            return true;
        }
        
        return false;
    }
    
    
    /**
     * Return a copy of this StemCell.
     *
     */
    public Object clone() throws CloneNotSupportedException
    {
        StemCell clone =  (StemCell)super.clone();
        
        return clone;
    }
        
        
    /**
     * toString() useful for debugging.
     *
     */
    public String toString()
    {
        String nl = System.getProperty("line.separator");
        
        StringBuffer buf = new StringBuffer();
        buf.append("cell(");
        buf.append(_id);
        if (!_alive)
        {
            buf.append(" dead");
        }

        double apopt = -1.0;
        if (_apoptosisEvent != null)
        {
            apopt = _apoptosisEvent.getTime();
        }
        if (_feedbackApoptosisEvent != null
            && _feedbackApoptosisEvent.getTime() < apopt)
        {
            apopt = _feedbackApoptosisEvent.getTime();
        }
        
        if (apopt > 0.0)
        {
            buf.append(" apopt:");
            buf.append(tform.format(apopt));
        }

        if (_cellDivisionEvent != null)
        {
            buf.append(" divt:");
            buf.append(tform.format(_cellDivisionEvent.getTime()));
            
        }
        
        buf.append(" apopr:");
        buf.append(dform.format(getApoptosisRate()));
        buf.append(" divr:");
        buf.append(dform.format(getDivisionRate()));
        buf.append(" mutr:");
        buf.append(sform.format(getMutationRate()));
        buf.append(" muts:");
        buf.append(_mutations);
        buf.append(")");
                   
        return buf.toString();
    }
    
    
    /**
     * Return an XML representation of this StemCell.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<stemcell>");
        
        pw.print("<alive>");
        pw.print(isAlive());
        pw.println("</alive>");

        if (isCancerous())
        {
            pw.print("<tsgknockout>");
            pw.print("true");
            pw.println("</tsgknockout>");
        }

        pw.print("<tsghits>");
        pw.print(getTSGHits());
        pw.println("</tsghits>");

        pw.print("<tsghitstac>");
        pw.print(getTSGHitsTAC());
        pw.println("</tsghitstac>");
        
        pw.println("<events>");
        if (getApoptosisEvent() != null)
        {
            pw.print(getApoptosisEvent().toXML());
        }
        if (getFeedbackApoptosisEvent() != null)
        {
            pw.print(getFeedbackApoptosisEvent().toXML());
        }
        if (getDivisionEvent() != null)
        {
            pw.print(getDivisionEvent().toXML());
        }
        if (getAsymmetricDivisionEvent() != null)
        {
            pw.print(getAsymmetricDivisionEvent().toXML());
        }
        pw.println("</events>");
        
        pw.println("<mutations>");
        for (Mutation m : getMutations())
        {
            pw.println(m.toXML());
        }
        pw.println("</mutations>");
        
        pw.println("<rates>");
        pw.println("<apoptosis>");
        pw.println(getApoptosisRate());
        pw.println("</apoptosis>");
        pw.println("<division>");
        pw.println(getDivisionRate());
        pw.println("</division>");
        pw.println("<mutation>");
        pw.println(getMutationRate());
        pw.println("</mutation>");
        pw.println("<feedbackapoptosis>");
        pw.println(getFeedbackApoptosisRate());
        pw.println("</feedbackapoptosis>");
        pw.println("<asymmetricdivision>");
        pw.println(getAsymmetricDivisionRate());
        pw.println("</asymmetricdivision>");
        pw.println("</rates>");
        
        pw.println("</stemcell>");
        
        return sw.toString();
    }
}

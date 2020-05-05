package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.sql.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


/**
 * Superclass for an XML result parser offering some static convenience methods
 * for navigating a DOM tree.
 *
 */
public class XMLResultParser
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    static boolean LOG_ENABLED = false;

    static DecimalFormat tform = new DecimalFormat("#.##");
    static DecimalFormat dform = new DecimalFormat("#.#######");


    ////////////////////////////////////////////////////////////////////////////
    // XML Parsing
    ////////////////////////////////////////////////////////////////////////////
    public static Element[] children(Node qParent)
    {
        List l = new LinkedList();
        NodeList nl = qParent.getChildNodes();
        log("nl", nl);
        for (int i=0; i<nl.getLength(); i++)
        {
            Node n = nl.item(i);
            if (n instanceof Element)
            {
                l.add(n);
            }
        }
        return (Element[])l.toArray(new Element[0]);
    }

    public static Element[] childrenByName(Node qParent, String qName)
    {
        List l = new LinkedList();
        NodeList nl = qParent.getChildNodes();
        log("childrenByName.nl", nl);
        for (int i=0; i<nl.getLength(); i++)
        {
            Node n = nl.item(i);
            if (n.getNodeName().equalsIgnoreCase(qName))
            {
                l.add(n);
            }
        }
        return (Element[])l.toArray(new Element[0]);
    }
    
    public static Element childByName(Node qParent, String qChildName)
    {
        NodeList nl = qParent.getChildNodes();
        log("childByName.nl", nl);
        for (int i=0; i<nl.getLength(); i++)
        {
            Node n = nl.item(i);
            if (n.getNodeName().equalsIgnoreCase(qChildName))
            {
                return (Element)n;
            }
        }
        return null;
    }
    
    public static String childTextVal(Node qParent, String qElementName)
    {
        StringBuffer buf = new StringBuffer();
        NodeList nl = qParent.getChildNodes();
        log("nl", nl);
        for (int i=0; i<nl.getLength(); i++)
        {
            Node n = nl.item(i);
            if (n.getNodeName().equalsIgnoreCase(qElementName))
            {
                return textVal(n);
            }
        }
        return null;
    }
    
    public static String textVal(Node qNode)
    {
        StringBuffer buf = new StringBuffer();
        NodeList nl = qNode.getChildNodes();
        log("nl", nl);
        for (int i=0; i<nl.getLength(); i++)
        {
            Node n = nl.item(i);
            if (n.getNodeName().equals("#text"))
            {
                buf.append(n.getNodeValue());
            }
        }
        return buf.toString().trim();
    }
    
    public static void log(String qMsg, NodeList qNodeList)
    {
        if (!LOG_ENABLED) return;
        
        log(qMsg + ":");
        for (int i=0; i<qNodeList.getLength(); i++)
        {
            Node n = qNodeList.item(i);
            log(i + " " + n.getNodeName() + " " + n.getNodeValue());
        }
    }

    public static void log(String $msg)
    {
        if (!LOG_ENABLED) return;

        System.err.println($msg);
    }
        
    static boolean isBlank(String qString)
    {
        return qString == null || qString.trim().length() == 0;
    }
    
    static String defaultVal(String qVal, String qDefaultVal)
    {
        if (isBlank(qVal))
        {
            return qDefaultVal;
        }
        return qVal;
    }
    
    static void assertName(Element $element, String $name)
    {
        if (!$element.getTagName().equalsIgnoreCase($name))
        {
            throw new IllegalArgumentException("expected tag name " + $name
                                               + " but was " 
                                               + $element.getTagName());
        }
    }
    
    
    
    /**
     * Count the number of mutator mutations in a given StemCell.
     *
     */
    static public int countMutatorMutations(Element $element)
    {
        int countMutator = 0;
        
        Node mutations = childByName($element, "mutations");
        if (mutations != null)
        {
            Element[] muts = childrenByName(mutations, "mutation");
            for (Element e : muts)
            {
                String type = childTextVal(e, "type");
                
                // handle non-summary mutations
                if (!type.endsWith("SummaryMutation"))
                {
                    // check for mutator mutation
                    if (type.endsWith("MutatorMutation"))
                    {
                        countMutator++;
                    }
                    
                    continue;
                }
                
                // check for mutator mutation
                String sumCountStr = childTextVal(e, "count");
                int sumCount = Integer.parseInt(sumCountStr);
                
                String label = childTextVal(e, "label");
                if (label.indexOf("mutatorSum") > -1 && sumCount > 0)
                {
                    countMutator += sumCount;
                }
            }
        }
        
        return countMutator;
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Parser
    ////////////////////////////////////////////////////////////////////////////
    
    /** The current RunStats object being computed by the parser */
    RunStats _current;
    
    
    public RunStats getRunStats()
    {
        return _current;
    }
    
    
    /**
     * Compile the statistics for a single .xml file object.
     *
     */
    public void compile(Document $doc)
    {
        _current = new RunStats();
        
        Element el = $doc.getDocumentElement();
        if (el.getTagName().equalsIgnoreCase("simulation"))
        {
            simulation(el);
        }
    }
    
    
    /**
     * Handle <simulation> tag.
     *
     */
    void simulation(Element $element)
    {
        assertName($element, "simulation");
        
        Node n = childByName($element, "simulationparams");
        simulationparams((Element)n);
        
        Element[] tissuesims = childrenByName($element, "tissuesim");
        for (Element e : tissuesims)
        {
            tissuesim(e);
        }
    }        
    
    
    /**
     * Handle <simulationparams> tag.
     *
     */
    void simulationparams(Element $element)
    {
        assertName($element, "simulationparams");
        
        Element props = childByName($element, "properties");
        
        Element[] entries = childrenByName(props, "entry");
        for (Element e : entries)
        {
            String key = e.getAttribute("key");
            String val = textVal(e).trim();
            
            _current.paramMap.put(key, val);
            
            if (key.equals("simulation.duration"))
                _current.simulationDuration = Double.parseDouble(val);
            
            if (key.equals("crypt.numcells.mean"))
                _current.stemcellsmean = Integer.parseInt(val);
            
            if (key.equals("cancer.uncontrolledgrowth.threshold"))
                _current.cancerthresh = Double.parseDouble(val);
            
            if (key.equals("mutation.rate.deleterious.apop"))
                _current.deleteriousApopRate = Double.parseDouble(val);
            
            if (key.equals("mutation.rate.deleterious.div"))
                _current.deleteriousDivRate = Double.parseDouble(val);
            
            if (key.equals("stemcell.apoptosisrate.base"))
                _current.apoptosisRate = Double.parseDouble(val);
            
            if (key.equals("stemcell.mutationrate.base"))
                _current.mutationRate = Double.parseDouble(val);
            
            if (key.equals("mutatormutation.mutation.multiplier"))
                _current.mutatorMultiplier = Double.parseDouble(val);
            
            if (key.equals("stemcell.tsgmutationrate.base"))
                _current.tsgMutationRate = Double.parseDouble(val);
        }
    }
    
    
    /**
     * Handle <tissuesim> tag.
     *
     */
    void tissuesim(Element $element)
    {
        assertName($element, "tissuesim");
        
        Node n = childByName($element, "lastevent");
        n = childByName(n, "event");
        String time = childTextVal(n, "time");
        time = time.trim();
        _current.time = Double.parseDouble(time);
        
        Element[] tissues = childrenByName($element, "tissue");
        for (Element e : tissues)
        {
            tissue(e);
        }
    }        
    
    
    /**
     * Handle <tissue> tag.
     *
     */
    void tissue(Element $element)
    {
        assertName($element, "tissue");
        
        Element tsghits = childByName($element, "tsghits");
        if (tsghits != null)
        {
            tsgHits(tsghits);
        }
        
        Element[] rows = childrenByName($element, "row");
        for (Element e : rows)
        {
            row(e);
        }
    }        
    
    
    /**
     * Handle <tsghits> tag.
     *
     */
    void tsgHits(Element $element)
    {
        assertName($element, "tsghits");
        
        double knockoutStem = -1.0;
        double knockoutTAC = -1.0;
        
        Element[] tsgs = childrenByName($element, "tsghit");
        for (Element e : tsgs)
        {
            String time = childTextVal(e, "time");
            String loc = childTextVal(e, "location").trim();
            Element stem = childByName(e, "stemcell");
            
            // grab 2nd hit if its in a tac and keep going
            if (knockoutTAC < 0
                && loc.equals("tac")
                && numTSGHits(stem) == 1)
            {
                knockoutTAC = Double.parseDouble(time);
                continue;
            }
            
            // grab 2nd stem cell hit.  should be last in list.
            if (knockoutStem < 0
                && loc.equals("stemcell")
                && numTSGHits(stem) == 2)
            {
                knockoutStem = Double.parseDouble(time);
                
                // gather stats about 2nd hit
                finalTSGHit(e);
                
                continue;
            }
        }
        
        _current.knockoutStem = knockoutStem;
        _current.knockoutTAC = knockoutTAC;
    }        
    
    
    /**
     * Handle <row> tag.
     *
     */
    void row(Element $element)
    {
        assertName($element, "row");
        Element[] crypts = childrenByName($element, "crypt");
        for (Element e : crypts)
        {
            crypt(e);
        }
    }        
    
    
    /**
     * Handle <crypt> tag.
     *
     */
    void crypt(Element $element)
    {
        assertName($element, "crypt");
        String cid = childTextVal($element, "id");
        
        // increment crypt count
        _current.totalCrypts++;

        // check for uncontrolled cell proliferation (old cancer)
        String outofcontrol = childTextVal($element, "unstable");
        if (outofcontrol != null)
        {
            if (Boolean.parseBoolean(outofcontrol))
            {
                _current.outofcontrol = true;
            }
        }
        
        // check for cancer
        boolean cancerCrypt = false;
        if (isCancerCrypt($element))
        {
            _current.cancerous = true;
            cancerCrypt = true;
        }
        
        // check for living crypt
        String alive = childTextVal($element, "alive");
        if (alive != null)
        {
            if (Boolean.parseBoolean(alive))
            {
                _current.livingCrypts++;
            }
        }

        // gather mutator statistics
        mutatorStats($element);

        // gather mutator statistics
        fitnessStats($element);
    }
    
    
    /**
     * Gather mutation statistics.
     *
     */
    void mutatorStats(Element $element)
    {    
        assertName($element, "crypt");

        // check for cancer
        boolean cancerCrypt = isCancerCrypt($element);
        
        Node stemcells = childByName($element, "stemcells");
        if (stemcells != null)
        {
            Element[] cells = childrenByName(stemcells, "stemcell");
            int mutatorStemCells = 0;
            int totalCells = 0;
            
            // for each stem cell, count mutator stem cells
            for (Element e : cells)
            {
                totalCells ++;
                
                int mutatorMutationCount = countMutatorMutations(e);
                
                if (mutatorMutationCount > 0)
                {
                    mutatorStemCells++;
                }
                
                // analyze stem cell
                stemcell(e, cancerCrypt);
            }
            
            System.err.println();
            
            // add to run-level count of mutator stem cells
            _current.stemCellsMutator += mutatorStemCells;
            
            // add to count of mutator crypts
            if (mutatorStemCells > 0)
            {
                _current.mutatorCrypts++;
                
                if (mutatorStemCells * 2 >= totalCells)
                {
                    _current.mutatorCrypts50++;
                }
                if (mutatorStemCells == totalCells)
                {
                    _current.mutatorCrypts100++;
                }
            }
            
            // record cancer crypt stats
            if (cancerCrypt)
            {
                _current.cancerCryptMutatorCells = mutatorStemCells;
                _current.cancerCryptTotalCells = cells.length;
                
                System.err.println(_current.cancerCryptMutatorCells
                                   + "/" + _current.cancerCryptTotalCells);
            }
        }
    }
    
    
    /**
     * Gather mutation statistics.
     *
     */
    void fitnessStats(Element $element)
    {    
        assertName($element, "crypt");
        
        // check for cancer
        boolean cancerCrypt = isCancerCrypt($element);
        
        // crypt fitness stats
        double divRate = 0.0;
        double apopRate = 0.0;
        int totalCells = 0;
        int mutatorStemCells = 0;
        
        Node stemcells = childByName($element, "stemcells");
        if (stemcells != null)
        {
            Element[] cells = childrenByName(stemcells, "stemcell");
            
            // for each stem cell, get apop and div rates
            for (Element e : cells)
            {
                totalCells++;
                
                divRate += divRate(e);
                apopRate += apopRate(e);
                
                int mutatorMutationCount = CancerStat.countMutatorMutations(e);
                
                if (mutatorMutationCount > 0)
                {
                    mutatorStemCells++;
                }
            }
            
            // record cancer crypt stats
            if (cancerCrypt)
            {
                _current.cancerCryptTotalCells = totalCells;
            }
        }
        
        // record fitness stats
        String cid = childTextVal($element, "id");

        CryptStats cs = new CryptStats();
        _current.cryptStats.add(cs);
        
        cs.id = cid;
        cs.cancer = cancerCrypt;
        cs.numCells = totalCells;
        if (totalCells > 0)
        {
            cs.divRate = divRate / (double)totalCells;
            cs.apopRate = apopRate / (double)totalCells;
        }
        
        // check for fixed mutator
        if (mutatorStemCells > 0 && mutatorStemCells == totalCells)
        {
            cs.fixedMutator = true;
        }
    }
    
    
    int numTSGHits(Element $element)
    {
        assertName($element, "stemcell");
        String ns = childTextVal($element, "tsghits");
        
        return Integer.parseInt(ns);
    }
    
    
    void finalTSGHit(Element $element)
    {
        assertName($element, "tsghit");
        
        String time = childTextVal($element, "time");
        
        Node cell = childByName($element, "stemcell");
        
        // count mutations in this stem cell
        Node mutations = childByName(cell, "mutations");
        if (mutations != null)
        {
            Element[] muts = childrenByName(mutations, "mutation");
            for (Element e : muts)
            {
                String type = childTextVal(e, "type");
                if (!type.equals("stemsim.object.SummaryMutation"))
                {
                    throw new RuntimeException("unrecognized mutation object");
                }
                
                String label = childTextVal(e, "label");
                String countStr = childTextVal(e, "count");
                int count = Integer.parseInt(countStr);
                
                if (label.equals("delApopSum"))
                {
                    _current.delApopMuts = count;
                }
                if (label.equals("delDivSum"))
                {
                    _current.delDivMuts = count;
                }
                if (label.equals("benApopSum"))
                {
                    _current.benApopMuts = count;
                }
                if (label.equals("benDivSum"))
                {
                    _current.benDivMuts = count;
                }
                if (label.equals("mutatorSum"))
                {
                    _current.mutMuts = count;
                }
            }
        }
        
        // gather cell rates
        Node rates = childByName(cell, "rates");
        if (rates != null)
        {
            String rateStr = null;
            double rate = 0.0;
            
            rateStr = childTextVal(rates, "apoptosis");
            rate = Double.parseDouble(rateStr);
            _current.apopRate = rate;
            
            rateStr = childTextVal(rates, "division");
            rate = Double.parseDouble(rateStr);
            _current.divRate = rate;
            
            rateStr = childTextVal(rates, "mutation");
            rate = Double.parseDouble(rateStr);
            _current.mutRate = rate;
            
            rateStr = childTextVal(rates, "feedbackapoptosis");
            rate = Double.parseDouble(rateStr);
            _current.feedbackApopRate = rate;
            
            rateStr = childTextVal(rates, "asymmetricdivision");
            rate = Double.parseDouble(rateStr);
            _current.asymmDivRate = rate;
        }
        
    }
    
    
    boolean isCancerCrypt(Element $element)
    {
        assertName($element, "crypt");
        
        Node stemcells = childByName($element, "stemcells");
        if (stemcells != null)
        {
            Element[] cells = childrenByName(stemcells, "stemcell");
            
            for (Element e : cells)
            {
                if (isCancerStemCell(e))
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    boolean isCancerStemCell(Element $element)
    {
        assertName($element, "stemcell");
        
        String cancerous = childTextVal($element, "tsgknockout");
        if (cancerous != null)
        {
            if (Boolean.parseBoolean(cancerous))
            {
                return true;
            }
        }
        
        return false;
    }        
    
    
    /**
     * Handle <stemcell> tag.
     *
     */
    void stemcell(Element $element, boolean $cancerCrypt)
    {
        assertName($element, "stemcell");
        
        // stem cells total count
        _current.totalStemCells++;
        
        // count of all mutations
        int count = 0;
        
        // count mutations in this stem cell
        Node mutations = childByName($element, "mutations");
        if (mutations != null)
        {
            Element[] muts = childrenByName(mutations, "mutation");
            for (Element e : muts)
            {
                String type = childTextVal(e, "type");
                
                // handle non-summary mutations
                if (!type.endsWith("SummaryMutation"))
                {
                    count++;
                    continue;
                }
                
                // handle summary mutations
                String sumCountStr = childTextVal(e, "count");
                int sumCount = Integer.parseInt(sumCountStr);
                count += sumCount;
            }
        }
        
        // add to total mutations count
        if ($cancerCrypt)
            _current.cancerMutationCounts.add(count);
        else
            _current.mutationCounts.add(count);

        // collect fitness of cancer stem cell
        if (isCancerStemCell($element))
        {
            _current.tsgcellApop = apopRate($element);
            _current.tsgcellDiv = divRate($element);
        }
        
        System.err.print(count + " ");
    }

    
    double rate(Element $element, String $rate)
    {
        assertName($element, "stemcell");
        
        Element rates = childByName($element, "rates");
        String rtext = childTextVal(rates, $rate);
        
        return Double.parseDouble(rtext);
    }
    
    
    double apopRate(Element $element)
    {
        return rate($element, "apoptosis");
    }
    
    
    double divRate(Element $element)
    {
        return rate($element, "division");
    }
    
}

package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import stemsim.util.*;


/**
 * GenericStat is an executable class that computes the cancer related 
 * statistics from XML final simulation state files.
 *
 */
public class GenericStat extends XMLResultParser
{
    ////////////////////////////////////////////////////////////////////////////
    // Object Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Directory containing simulation result files */
    File _dataDir = null;
    
    /** List of RunStats objects, one per simulation run */
    List<RunStats> _stats = new LinkedList<RunStats>();
    
    /** The current RunStats object being computed by the parser */
    RunStats _current;

    public GenericStat(File $dataDir)
    {
        _dataDir = $dataDir;
    }
    
    public File getDataDir()
    {
        return _dataDir;
    }
    
    protected String filename(String $suffix)
    {
        return _dataDir.getName() + $suffix;
    }
    
    
    static SortedSet<Integer> numCells(List<RunStats> $stats)
    {
        SortedSet<Integer> ncs = new TreeSet<Integer>();
        
        for (RunStats rs : $stats)
        {
            int nc = rs.stemcellsmean;
            ncs.add(nc);
        }
        
        return ncs;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Stat Methods
    ////////////////////////////////////////////////////////////////////////////

    public void compile()
    throws javax.xml.parsers.ParserConfigurationException,
    java.io.IOException, org.xml.sax.SAXException
    {
        // recursively find all .xml files in data directory
        List<File> rfiles = new LinkedList<File>();
        FileUtil.recursiveGetFiles(_dataDir, 
                                   new FileUtil.DotXMLFilter(), rfiles);
        
        // loop through each xml file, build DOM tree and compute stats
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder dom = dbf.newDocumentBuilder();

        for (File rfile : rfiles)
        {
            System.err.println();
            System.err.println(rfile.getCanonicalPath());
            
            Document doc = dom.parse(rfile);
            compile(doc);
        }
    }
    
    
    /**
     * Compile the statistics for a single .xml file object.
     *
     */
    public void compile(Document $doc)
    {
        XMLResultParser parser = new XMLResultParser();
        parser.compile($doc);
        _stats.add(parser.getRunStats());
    }
    
    
    /**
     * Return a list of valid RunStats objects.  Valid RunStats objects
     * represent simulation runs that have run to duration or have
     * developed cancer.
     *
     */
    List<RunStats> validStats()
    {
        List<RunStats> valids = new ArrayList<RunStats>(_stats.size());
        for (RunStats stats : _stats)
        {
            // if this is true, it means i stopped the sim
            if (stats.time < stats.simulationDuration 
                && !stats.cancerous 
                && stats.livingCrypts > 0)
            {
                System.err.println("rejected: " + stats.time);
                continue;
            }
            valids.add(stats);
        }
        return valids;
    }

}

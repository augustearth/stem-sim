package stemsim.statxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;
import java.text.DecimalFormat;

import stemsim.object.*;

public class CryptStats
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    
    static int numCols(List<CryptStats> $crypts)
    {
        // TRICKY: IE, stupid.  Because I didn't include coordinates in the XML
        // output, we have to "know" how the tissue was constructed based on the
        // total number of crypts in the XML result file.  Fortunately, we do
        // know, but this really stinks.
        int mod = -1;
        switch ($crypts.size()) 
        {
            case 25:  mod = 5; break;
            case 180:  mod = 18; break;
            case 90:  mod = 9; break;
            case 36:  mod = 6; break;
            case 18:  mod = 3; break;
            case 9:  mod = 3; break;
            case 6:  mod = 3; break;
            case 2:  mod = 2; break;
            case 1:  mod = 1; break;
                
            default: 
                throw new IllegalArgumentException("" + $crypts.size()); 
        }
        return mod;
    }

    static int numRows(List<CryptStats> $crypts)
    {
        return Math.round($crypts.size() / numCols($crypts));
    }
    
    static List<CryptStats> getRow(List<CryptStats> $crypts, int i)
    {
        int numcols = numCols($crypts);
        int start = i * numcols;
        
        // this is one past the last element so we can pass directly to
        // subList which wants from (inclusive) and to (exclusive)
        int end = start + numcols;
        
        return $crypts.subList(start, end);
    }
    
    static void setHexPoints(List<CryptStats> $crypts)
    {
        int numcols = numCols($crypts);
        int numrows = numRows($crypts);
        
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
            int rowi = numrows - y - 1;
            List<CryptStats> row = getRow($crypts, rowi);
            
            if (shiftc-- < 0)
            {
                shiftc = 0;
                basex--;
            }
            
            int x = basex;
            
            for (CryptStats c : row)
            {
                c.setHexPoint(x++, y);
            }
        }
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    String id;
    
    HexPoint hexPoint;
    
    int numCells;
    boolean cancer;
    double divRate;
    double apopRate;
    boolean fixedMutator = false;
    
    public void setHexPoint(int $x, int $y)
    {
        hexPoint = new HexPoint($x, $y);
    }
    
    public HexPoint getHexPoint()
    {
        return hexPoint;
    }
    
    public int hexDistance(CryptStats $c)
    {
        HexPoint a = getHexPoint();
        HexPoint b = $c.getHexPoint();
        
        return a.distance(b);
    }
    
    double fitness()
    {
        return divRate - apopRate;
    }
    
    public String toString()
    {
        return id + hexPoint;
    }
}

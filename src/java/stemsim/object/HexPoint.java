package stemsim.object;


import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.*;
import java.text.DecimalFormat;

import stemsim.event.*;
import stemsim.simulation.*;


public class HexPoint implements Cloneable
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////

    static public int sign(int $i)
    {
        if ($i >=0 ) return +1;
        
        return -1;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////

    int _x = 0;
    int _y = 0;
    
    public HexPoint(int $x, int $y)
    {
        _x = $x;
        _y = $y;
    }
    
    public int x()
    {
        return _x;
    }
    
    public int y()
    {
        return _y;
    }

    public int distance(HexPoint $b)
    {
        HexPoint a = this;
        HexPoint b = $b;
        
        int dx = b.x() - a.x();
        int dy = b.y() - a.y();
        
        int dist = -1;
        if (sign(dx) == sign(dy))
        {
            dist = Math.max(Math.abs(dx), Math.abs(dy));
        }
        else
        {
            dist = Math.abs(dx) + Math.abs(dy);
        }
        
        return dist;
    }
    
    public Object clone() throws CloneNotSupportedException
    {
        HexPoint clone = (HexPoint)super.clone();
        
        clone._x = _x;
        clone._y = _y;
        
        return clone;
    }
    
    public String toString()
    {
        return "<" + _x + "," + _y + ">";
    }
}

package stemsim.simulation;


import java.io.*;
import java.util.*;


/**
 * Oject to manage simulation parameters -- read them from a properties file
 * and provide accessor methods.
 *
 * This is a somewhat inefficient implementation in that there are not
 * specific methods for specific parameters.  Rather, parameters are accessed
 * through generic methods given the parameter name.  This is helpful from a
 * development perspective, but less than optimal in efficiency.
 *
 */
public class SimulationParams
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    static public SimulationParams load(File $file)
        throws FileNotFoundException, IOException
    {
        FileInputStream in = new FileInputStream($file);
        Properties props = new Properties();
        props.load(in);
        
        return new SimulationParams(props);
    }

    
    ////////////////////////////////////////////////////////////////////////////
    // Instance Level
    ////////////////////////////////////////////////////////////////////////////
    
    /** Properties object containing all parameters */
    Properties _props = null;
    
    /** A cache for floating point parameter values */
    Map<String,Double> _doubleCache = new HashMap<String,Double>();

    /** A cache for integer parameter values */
    Map<String,Integer> _integerCache = new HashMap<String,Integer>();

    /** A cache for Boolean parameter values */
    Map<String,Boolean> _booleanCache = new HashMap<String,Boolean>();


    public SimulationParams()
    {
        _props = new Properties();
    }

    public SimulationParams(Properties $props)
    {
        if ($props == null)
        {
            throw new IllegalArgumentException("$props cannot be null");
        }
        _props = $props;
    }
    
    public void setProperty(String $key, String $value)
    {
        _props.setProperty($key, $value);
        
        _doubleCache.remove($key);
    }

    public boolean isConfigured(String $param)
    {
        String val = _props.getProperty($param);
        return (val != null);
    }
    
    public String getString(String $param)
    {
        String val = _props.getProperty($param);
        if (val == null)
        {
            throw new IllegalArgumentException("no param: " + $param);
        }
        return val;
    }
    
    public double getDouble(String $param)
    {
        if (_doubleCache.containsKey($param))
        {
            return _doubleCache.get($param);
        }
        
        String val = getString($param);
        double d = Double.parseDouble(val);
        _doubleCache.put($param, d);
        
        return d;
    }
    
    public int getInt(String $param)
    {
        if (_integerCache.containsKey($param))
        {
            return _integerCache.get($param);
        }

        String val = getString($param);
        int i = Integer.parseInt(val);
        _integerCache.put($param, i);
        
        return i;
    }

    public boolean getBoolean(String $param)
    {
        if (_booleanCache.containsKey($param))
        {
            return _booleanCache.get($param);
        }

        String val = getString($param);
        boolean b = (Boolean.valueOf(val)).booleanValue();
        _booleanCache.put($param, b);
        
        return b;
    }
    
    
    public boolean isEnabled(String $prop)
    {
        if (!$prop.endsWith(".enabled"))
        {
            $prop += ".enabled";
        }
        
        return isTrue($prop);
    }

    public boolean isTrue(String $prop)
    {
        return (isConfigured($prop) && getBoolean($prop));
    }
    
    /**
     * Return an xml representation of this object.
     *
     */
    public String toXML()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("<simulationparams>");
        
        try
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            _props.storeToXML(os, "");
            
            // remove first two lines
            StringReader sr = new StringReader(os.toString());
            BufferedReader in = new BufferedReader(sr);
            in.readLine();
            in.readLine();
            String line = null;
            while ((line = in.readLine()) != null)
            {
                pw.println(line);
            }
        }
        catch (IOException e)
        {
            // this should never happen
            throw new RuntimeException(e);
        }
        
        pw.println("</simulationparams>");
        
        return sw.toString();
    }
}

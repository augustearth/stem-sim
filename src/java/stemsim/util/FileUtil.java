package stemsim.util;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.xml.parsers.*;


/**
 * A util class providing a convenience method to recursivel get XML files.
 *
 */
public class FileUtil
{
    ////////////////////////////////////////////////////////////////////////////
    // Class Level
    ////////////////////////////////////////////////////////////////////////////
    static public class DotXMLFilter implements FilenameFilter
    {
        public boolean accept(File $dir, String $name)
        {
            return $name.endsWith(".xml");
        }
    }
    

    static public class ExactMatchFilter implements FilenameFilter
    {
        String _name = null;
        
        public ExactMatchFilter(String $name)
        {
            _name = $name;
        }
        
        public boolean accept(File $dir, String $name)
        {
            return $name.equals(_name);
        }
    }

    
    
    static public void recursiveGetFiles(File $file, 
                                         FilenameFilter $filter, 
                                         List<File> $list)
    {
        if ($file.isFile())
        {
            if ($filter.accept($file.getParentFile(), $file.getName()))
            {
                $list.add($file);
            }
            return;
        }
        
        File[] files = $file.listFiles();
        for (File f : files)
        {
            recursiveGetFiles(f, $filter, $list);
        }
    }
    
    //<entry key="crypt.numcells.mean">90</entry>
    static public String getStemSimParameter(File $file,
                                             String $paramName)
    throws FileNotFoundException, IOException
    {
        Pattern p = Pattern.compile("<entry key=\""
                                    + "(.*)\">"
                                    + "(.*)"
                                    + "</entry>");

        BufferedReader in = new BufferedReader(new FileReader($file));
        String line = null;
        while ((line = in.readLine()) != null)
        {
            Matcher m = p.matcher(line);
            
            if (m.find())
            {
                String key = m.group(1);
                String val = m.group(2);
                
                if (key.equalsIgnoreCase($paramName))
                    return val;
            }
        }
        
        return null;
    }

}

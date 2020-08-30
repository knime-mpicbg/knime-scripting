package de.mpicbg.knime.knutils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class FileUtils {

    /**
     * Recursively parses a directory and put all files in the return list which match ALL given filters.
     */
    public static List<File> findFiles(File directory) {
        return findFiles(directory, true);
    }


    /**
     * Collects all files list which match ALL given filters.
     *
     * @param directory   the base directory for the search
     * @param beRecursive
     * @return
     */
    public static List<File> findFiles(File directory, boolean beRecursive) {
        assert directory.isDirectory();

        List<File> allFiles = new ArrayList<File>();

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (beRecursive)
                    allFiles.addAll(findFiles(file));
            } else {
                if (!file.isHidden()) {
                    allFiles.add(file);
                }
            }
        }

        Collections.sort(allFiles);

        return allFiles;
    }
    
    /**
     * counts the number of lines of a text-file
     * should be a fast solution according to stackoverflow
     * https://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
     * 
     * @param filename
     * @return
     * @throws IOException
     */
    public static int countLines(FileInputStream fis) throws IOException {
    	InputStream is = new BufferedInputStream(fis);
    	try {
    		byte[] c = new byte[1024];

    		int readChars = is.read(c);
    		if (readChars == -1) {
    			// bail out if nothing to read
    			return 0;
    		}

    		// make it easy for the optimizer to tune this loop
    		int count = 0;
    		while (readChars == 1024) {
    			for (int i=0; i<1024;) {
    				if (c[i++] == '\n') {
    					++count;
    				}
    			}
    			readChars = is.read(c);
    		}

    		// count remaining characters
    		while (readChars != -1) {
    			for (int i=0; i<readChars; ++i) {
    				if (c[i] == '\n') {
    					++count;
    				}
    			}
    			readChars = is.read(c);
    		}

    		return count == 0 ? 1 : count;
    	} finally {
    		is.close();
    	}
    }
    
    /**
     * retrieve text file content from resource input stream
     * 
     * @param stream		{@link ClassLoader#getResourceAsStream(String)}
     * @return				String with file content
     * 
     * @throws IOException
     */
    public static String readFromRessource(InputStream stream) throws IOException {
    	StringBuilder sb = new StringBuilder();
        String line;
     
        // try-with-resources
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
        	while ((line = reader.readLine()) != null) {
        		sb.append(line);
        		sb.append(System.getProperty("line.separator", "\r\n"));
        	}
        }
       
        return sb.toString();
    }
}

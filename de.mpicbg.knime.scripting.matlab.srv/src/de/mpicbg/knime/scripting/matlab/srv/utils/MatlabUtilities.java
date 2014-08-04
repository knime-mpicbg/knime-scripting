/*
 * Copyright (c) 2011.
 * Max Planck Institute of Molecular Cell Biology and Genetics, Dresden
 *
 * This module is distributed under the BSD-License. For details see the license.txt.
 *
 * It is the obligation of every user to abide terms and conditions of The MathWorks, Inc. Software License Agreement.
 * In particular Article 8 “Web Applications”: it is permissible for an Application created by a Licensee of the
 * NETWORK CONCURRENT USER ACTIVATION type to use MATLAB as a remote engine with static scripts.
 */

package de.mpicbg.knime.scripting.matlab.srv.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Collection of utility methods
 * 
 * @author Holger Brandl, Felix Meyenhofer
 */
@Deprecated
public class MatlabUtilities {
	
	/** Name of the m-file to load the hashmap object dump from KNIME */
	public final static String MATLAB_HASHMAP_SCRIPT = "hashmaputils.m";
	
	
	/**
	 * Converts an array of doubles to the MATLAB array declaration string.
	 * 
	 * @param vector of numbers
	 * @return String that, when executed in MATLAB, produces vector with the numbers of the input vector 
	 */
    public static StringBuffer convert2StringVector(double[] vector) {
        StringBuffer stringVec = new StringBuffer("[");
        for (double value : vector) {
            stringVec.append(value).append(", ");
        }
        stringVec.deleteCharAt(stringVec.length() - 1).append("]");
        return stringVec;
    }
    
    
    /**
     * Copy the MATLAB script to load the Java HashMap object dump
     * to a folder accessible for MATLAB (usually the JVM's temp-folder).
     * 
     * @param destinationDirectory for the MATLAB script
     * @return script name without extension
     */
    public static String transferHashMapMFile(String destinationDirectory) {
    	copyResource(MATLAB_HASHMAP_SCRIPT, destinationDirectory);
    	return fileName2functionName(MATLAB_HASHMAP_SCRIPT);
    }
    
    
    /**
     * Copies a given resource file to a destination directory
     * 
     * @param resourceAbsolutePath absolute path to the resource file (class path as root)
     * @param destinationDirectory directory to copy the file to (uses the file name from the resource)
     * @return
     */
    public static File copyResource(String resourceAbsolutePath, String destinationDirectory) {
    	// Create a file from the resource path to easily extract the file name
		File resfile = new File(resourceAbsolutePath);
		
		// Create the output file		
		File outfile = new File(destinationDirectory, resfile.getName());
		
//		if (!outfile.exists()) {
			outfile.deleteOnExit();
	        
	        // Get the thread class loader to access the resource file
	        ClassLoader loader = Thread.currentThread().getContextClassLoader();
	        InputStream resstream = loader.getResourceAsStream(resourceAbsolutePath);
	        
	        try {
				writeStreamToFile(resstream, new FileOutputStream(outfile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
//		}
        
        return outfile;
    }
    
    
    /**
     * Write an file input to an output stream.
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    public static void writeStreamToFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16384];
        while (true) {
            int count = in.read(buffer);
            if (count < 0)
                break;
            out.write(buffer, 0, count);
        }
        in.close();
        out.close();
    }
    
    /**
     * Clip the extension of the file name so it can be used as a function name 
     * in MATLAB. 
     * 
     * @param str file name
     * @return	function name
     */
    public static String fileName2functionName(String str) {
        File tmp = new File(str);
        str = tmp.getName();
        int index = str.indexOf(".");
        if (index > 1) {
            str = str.substring(0, index);
        }
        return str;
    }
    
}


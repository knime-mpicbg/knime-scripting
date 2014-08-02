package de.mpicbg.knime.scripting.matlab.srv.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.mpicbg.knime.scripting.matlab.srv.Matlab;


/**
 * This class serves to regroup all the MATLAB code generation and manipulations
 * 
 * @author Felix Meyenhofer
 */
public class MatlabCode {
	
	/** MATLAB code snippet */
	private String snippet = "";
	
	/** Temp-file containing the MATLAB code */
	private File snippetfile;
	
	/** Temp-file-name without the file-extension */
	private String snippetfun;
	
	/** Temp-file of the KNIME data */
	private File tablefile;
	
	/** Temp-file for the MATLAB plot output */
	private File plotfile;
	
	
	/** Data type to be used to store the KNIME table data */
	private String type = Matlab.DEFAULT_TYPE;
	
	/** JVM temp-directory */
	private final String temppath = Matlab.TEMP_PATH;
	
	/** Path to the MATLAB script to handle the data loading */
	private final String hashresource = Matlab.MATLAB_HASHMAP_SCRIPT;
	
	/** Function name of the MATLAB script that handles the data loading */
	private final String hashfun = fileName2functionName(this.hashresource);
	
	
	/**
	 * Constructor 
	 * 
	 * @param tableFile KNIME data table
	 * @param matlabType MATLAB type (for the variable that holds the table)
	 */
	public MatlabCode(File tableFile, String matlabType) {
		this.tablefile = tableFile;
		this.type = matlabType;
	}
	
	/**
	 * Constructor 
	 * 
	 * @param code User defined MATLAB code 
	 * @param tableFile KNIME data table
	 */
	public MatlabCode(String code, File tableFile, String matlabType) {
		this.snippet = code;
		this.tablefile = tableFile;
		this.type = matlabType;
	}
	
	
	/**
	 * Wrap the entire snippet in a try-catch clause to for error handling
	 * 
	 * @return Modified code
	 */
//	private String addErrorHandlingCode() {
//		String str = "try;" + this.snippet + "catch err;end";
//		return str;
//	}
	
	/**
	 * Add the code to make a function definition out of a MATLAB script
	 *  
	 * @return Modified code
	 * @throws Exception 
	 */
	private String addFunctionCode(boolean hasOutput) throws Exception {
		if (this.snippetfile == null)
			throw new Exception("Before addning the function header to the snippet, A file needs to be created.");
		
		this.snippetfun = fileName2functionName(this.snippetfile.getName());
		
		if (hasOutput) {
			return "function "+ Matlab.OUTPUT_VARIABLE_NAME +"=" + this.snippetfun + "()\n" + 
					this.snippet;
		} else {
			return "function " + this.snippetfun + "()\n" + this.snippet;
		}
	}
	
	/**
	 * Add the code to create an invisible MATLAB figure (for the plot)
	 * and the bits to save the plot to a png-file.
	 * 
	 * @param code MATLAB plot code
	 * @param plotWidth MATLAB figure width
	 * @param plotHeight MATLAB figure height
	 * @return Modified code
	 */
	private String addPlotCode(String code, Integer plotWidth, Integer plotHeight) {
    	return "figureHandle = figure('visible', 'off', 'units', 'pixels', 'position', [0, 0, " + plotWidth + ", " + plotHeight + "]);\n" +
        		"set(gcf,'PaperPositionMode','auto');\n" +
        		code +
        		"print(figureHandle, '-dpng', '" + this.plotfile + "');\n" + 
        		Matlab.OUTPUT_VARIABLE_NAME + "=[];";							// so it conforms with the function signature
	}
	
	/**
	 * Add a message to be displayed in the MATLAB command window informing the user
	 * on what happened after the execution of the {@link OpenInMatlab} node
	 *  
	 * @param code MATLAB snippet
	 * @return Modified code
	 */
	private String addOpenMessage(String code) {
		return "disp('The data is available as the following variables in the Workspace:');" +
        		"disp('kIn :  " + this.type + " containing the KNIME table.');" + 
        		"disp('names: structure containing column header information.');" +
        		"disp('To reload the KNIME table simply re-execute the OpenInMatlab node.');" +
        		code;
	}

	/**
	 * Add the code needed to load the object dump of the hash-map 
	 * (KNIME table)
	 * 
	 * @param code MATLAB snippet
	 * @return Modified code
	 * @throws Exception
	 */
	private String addLoadCode(String code) throws Exception {
		if (this.tablefile == null)
			throw new Exception("Table file name is missing!");
		
		return "cd " + this.temppath+ ";\n" + 
				"[" + Matlab.INPUT_VARIABLE_NAME +",names]=" + this.hashfun + "('" + this.tablefile.getAbsolutePath() + "','" + this.type + "');\n" +
				code;
	}
	
	/**
	 * Add the code to save the data in {@link Matlab#OUTPUT_VARIABLE_NAME}
	 * to a binary file.
	 *  
	 * @param code MATLAB snippet
	 * @return Modified code
	 */
	public String addSaveCode(String code) {
		return code + "\n" +
				"cd " + Matlab.TEMP_PATH + ";\n" +
				this.hashfun + "('" + this.tablefile.getAbsolutePath() + "', " + Matlab.OUTPUT_VARIABLE_NAME +");";
	}
	
	/**
	 * Prepare the snippet for the open-in-MATLAB task, by adding all
	 * lines that have to be executed in the MATLAB application. 
	 * 
	 * @return Modified code
	 * @throws Exception
	 */
	public String prepareOpenCode() throws Exception  {
		copyHashMapScriptToTempDirectory();
		this.snippet = addLoadCode(this.snippet);
		this.snippet = addOpenMessage(this.snippet);
		return this.snippet;
	}
	
	/**
	 * Prepare the snippet for the execution in the MATLAB application, 
	 * by adding the code to load the data and save it back to a binary file.
	 * 
	 * @return Modified code
	 * @throws Exception
	 */
	public String prepareSnippetCode() throws Exception  {
		copyHashMapScriptToTempDirectory();
		this.snippet = addLoadCode(this.snippet);
		this.snippet = addSaveCode(this.snippet);
		return copySnippetToTempDirectory(true);
	}
	
	/**
	 * Prepare a plot snippet for the execution in MATLAB application, 
	 * by adding the code necessary to load the data, catch the plot and
	 * save it to a temporary png-file.
	 * 
	 * @param width Plot width
	 * @param height Plot height
	 * @return	Modified code
	 * @throws Exception
	 */
	public String preparePlotCode(int width, int height) throws Exception {
		copyHashMapScriptToTempDirectory();
		this.plotfile = File.createTempFile(Matlab.PLOT_TEMP_FILE_PREFIX, Matlab.PLOT_TEMP_FILE_SUFFIX);
		this.plotfile.deleteOnExit();
		this.snippet = addLoadCode(this.snippet);
		this.snippet = addPlotCode(this.snippet, width, height);
		return copySnippetToTempDirectory(false);		
	}

	/**
	 * Clean the files data to liberate disk space and memory.
	 */
	public void cleanup() {
		if (this.snippetfile != null)
			this.snippetfile.delete();
		if (this.tablefile != null)
			this.tablefile.delete();
		if (this.plotfile != null)
			this.plotfile.delete();
	}
	
	/**
	 * Get the {@link File} object pointing to the png-temp-file
	 * containing the MATLAB plot
	 * 
	 * @return MATLAB plot file
	 */
	public File getPlotFile() {
		return this.plotfile;
	}
	
	/**
	 * Copy the snippet string into a temporary MATLAB script.
	 * 
	 * @return MATLAB code to execute this script.
	 * @throws Exception 
	 */
	private String copySnippetToTempDirectory(boolean hasOutput) throws Exception {
    	this.snippetfile = File.createTempFile(Matlab.SNIPPET_TEMP_FILE_PREFIX, Matlab.SNIPPET_TEMP_FILE_SUFFIX);
    	this.snippetfile.deleteOnExit();
    	this.snippet = this.addFunctionCode(hasOutput);
    	InputStream inStream = new ByteArrayInputStream(this.snippet.getBytes());
    	writeStreamToFile(inStream, new FileOutputStream(this.snippetfile));
    	if (hasOutput) {
    		return "cd " + Matlab.TEMP_PATH + ";" + Matlab.OUTPUT_VARIABLE_NAME + "=" + this.snippetfun + ";\n";
    	} else {
    		return "cd " + Matlab.TEMP_PATH + ";" + this.snippetfun + ";\n";
    	}
    }
	
	/**
	 * Copy the MATLAB script to load and save input and output data of the ]
	 * snippet.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void copyHashMapScriptToTempDirectory() throws FileNotFoundException, IOException {
		copyResourceToTempDirectory(this.hashresource);
	}
	
	/**
	 * Copy a resource to the JVM temp direcotory.
	 * 
	 * @param resourceAbsolutePath Absolute path to the resource file in this package.
	 * @return {@link File} pointing to the copy of the resource file.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private File copyResourceToTempDirectory(String resourceAbsolutePath) throws FileNotFoundException, IOException {
		File resfile = new File(resourceAbsolutePath);	
		File outfile = new File(this.temppath, resfile.getName());
		outfile.deleteOnExit();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream resstream = loader.getResourceAsStream(resourceAbsolutePath);
        writeStreamToFile(resstream, new FileOutputStream(outfile));
        return outfile;
    }
	
	/**
     * Clip the extension of the file name so it can be used as a function name 
     * in MATLAB. 
     * 
     * @param str file name
     * @return function name
     */
    private static String fileName2functionName(String str) {
        File tmp = new File(str);
        str = tmp.getName();
        int index = str.indexOf(".");
        if (index > 1) {
            str = str.substring(0, index);
        }
        return str;
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
	
}


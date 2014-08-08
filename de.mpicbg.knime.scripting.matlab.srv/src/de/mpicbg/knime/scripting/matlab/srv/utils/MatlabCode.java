package de.mpicbg.knime.scripting.matlab.srv.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;

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
	
	
	public MatlabCode(String matlabType) {
		this.type = matlabType;
	}
	
	/**
	 * 
	 * @param code
	 * @param matlabType
	 */
	public MatlabCode(String code, String matlabType) {
		this.snippet = code;
		this.type = matlabType;
	}
	
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
	private String addErrorHandlingCode() {
		return "\ntry\n" + this.snippet + "\ncatch " + Matlab.ERROR_VARIABLE_NAME + ";end\n";
	}
	
	/**
	 * Add the code to make a function definition out of a MATLAB script
	 *  
	 * @return Modified code
	 * @throws Exception 
	 */
	private String addFunctionSignatureToCode(boolean hasInput, boolean hasOutput) throws Exception {
		if (this.snippetfile == null)
			throw new Exception("Before addning the function header to the snippet, A file needs to be created.");
		
		this.snippetfun = fileName2functionName(this.snippetfile.getName());
		
		return "function " + createFunctionSignature(hasInput, hasOutput) + "\n" + Matlab.ERROR_VARIABLE_NAME + "=struct('identifier', '', 'message', '');\n" + this.snippet;
	}
	
	/**
	 * Create a function signature so the snippet can be packed in a m-file and called as a
	 * function
	 * 
	 * @param hasInput
	 * @param hasOutput
	 * @return
	 */
	private String createFunctionSignature(boolean hasInput, boolean hasOutput) {
		String signature = "";
		if (hasOutput)
			signature += "[" + Matlab.OUTPUT_VARIABLE_NAME + "," + Matlab.ERROR_VARIABLE_NAME + "]" +"=" + this.snippetfun;
		else
			signature += Matlab.ERROR_VARIABLE_NAME + "=" + this.snippetfun;
		
		if (hasInput)
			signature += "(" + Matlab.INPUT_VARIABLE_NAME + ")";
		else
			signature += "()";
		
		return signature;
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
        		code + "\n" +
        		"print(figureHandle, '-dpng', '" + this.plotfile + "');\n" + 
        		Matlab.OUTPUT_VARIABLE_NAME + "=[];";							// so it conforms with the function signature
	}
	
	/**
	 * Add the plot code (Remote version)
	 * 
	 * @param plotWidth
	 * @param plotHeight
	 * @return
	 */
	public String addPlotCode(Integer plotWidth, Integer plotHeight) {
		return addPlotCode(this.snippet, plotWidth, plotHeight);
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
        		"disp('columnNames: structure containing column header information.');" +
        		"disp('             Depending on the MATLAB data type strings used KNIME might need slight modification!');" +
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
			return code;
		
		return "cd " + this.temppath+ ";\n" + 
				"[" + Matlab.INPUT_VARIABLE_NAME +"," + Matlab.COLUMNS_VARIABLE_NAME + "]=" + 
				this.hashfun + "('" + this.tablefile.getAbsolutePath() + "','" + this.type + "');\n" +
				code;
	}
	
	/**
	 * Add the code to save the data in {@link Matlab#OUTPUT_VARIABLE_NAME}
	 * to a binary file.
	 *  
	 * @param code MATLAB snippet
	 * @return Modified code
	 */
	private String addSaveCode(String code) {
		if (this.tablefile == null)
			return code;
		
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
	public String prepareOpenCode(boolean hasInput) throws Exception  {
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
	public String prepareSnippetCode(boolean hasInput) throws Exception  {
		copyHashMapScriptToTempDirectory();
		this.snippet = addLoadCode(this.snippet);
		this.snippet = addSaveCode(this.snippet);
		this.snippet = addErrorHandlingCode();
		return copySnippetToTempDirectory(hasInput, true);
	}
	
	/**
	 * Prepare a plot snippet for the execution in MATLAB application, 
	 * by adding the code necessary to load the data, catch the plot and
	 * save it to a temporary png-file.
	 * 
	 * @param width Plot width
	 * @param height Plot height
	 * @param hasInput flag to signal if the data table is a script input or not
	 * @return	Modified code
	 * @throws Exception
	 */
	public String preparePlotCode(int width, int height, boolean hasInput) throws Exception {
		copyHashMapScriptToTempDirectory();
		createPlotFile();
		this.snippet = addLoadCode(this.snippet);
		this.snippet = addPlotCode(this.snippet, width, height);
		this.snippet = addErrorHandlingCode();
		return copySnippetToTempDirectory(hasInput, false);		
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
	 * Create a file for the plot output in the JVM temp-folder
	 * 
	 * @return
	 * @throws IOException
	 */
	public File createPlotFile() throws IOException {
		this.plotfile = File.createTempFile(Matlab.PLOT_TEMP_FILE_PREFIX, Matlab.PLOT_TEMP_FILE_SUFFIX);
		this.plotfile.deleteOnExit();
		return this.plotfile;
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
	private String copySnippetToTempDirectory(boolean hasInput, boolean hasOutput) throws Exception {
    	this.snippetfile = File.createTempFile(Matlab.SNIPPET_TEMP_FILE_PREFIX, Matlab.SNIPPET_TEMP_FILE_SUFFIX);
    	this.snippetfile.deleteOnExit();
    	this.snippet = this.addFunctionSignatureToCode(hasInput, hasOutput);
    	InputStream inStream = new ByteArrayInputStream(this.snippet.getBytes());
    	writeStreamToFile(inStream, new FileOutputStream(this.snippetfile));
    	
    	return "cd " + Matlab.TEMP_PATH + ";" + createFunctionSignature(hasInput, hasOutput) + ";";
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
    private static void writeStreamToFile(InputStream in, OutputStream out) throws IOException {
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
	
    
    public static List<String> getVariableNamesFromColumnNames(String type, List<String> colNames) {
    	if (type.equals("dataset"))
    		return colNames;
    	if (type.equals("map"))
    		return colNames;
    	if (type.equals("struct")){
    		List<String> varNames = new ArrayList<String>();
    		for (String colName : colNames) 
    			varNames.add(colName.replaceAll("[^0-9a-zA-Z_]", ""));
    		return varNames;
    	}
    	
    	return null;
    }
    
    public static String getInputVariableInstanciationCommand(String type, List<String> vars, List<DataType> types) {
    	if (type.equals("dataset")) {
    		String cmd = Matlab.INPUT_VARIABLE_NAME + "= dataset(";
    		for (int i = 0; i < vars.size(); i++)
    			cmd += "[],";

    		return cmd.substring(0, cmd.length()-1) + ");";
    	}
    	if (type.equals("map")){
    		String cmd = Matlab.INPUT_VARIABLE_NAME + "=containers.Map;";
    		String empty;
    		for (int i = 0; i < vars.size(); i++) {
    			if (types.get(i).equals(StringCell.TYPE))
    				empty = "{}";
    			else
    				empty = "[]";
    			cmd += Matlab.INPUT_VARIABLE_NAME + "('"+ vars.get(i) +"')=" + empty + ";";
    		}
    		return cmd;
    	}
    	if (type.equals("struct")) {
    		String cmd = "";
    		String empty;
    		for (int i = 0; i < vars.size(); i++) {
    			if (types.get(i).equals(StringCell.TYPE))
    				empty = "{}";
    			else
    				empty = "[]";
    			cmd += Matlab.INPUT_VARIABLE_NAME + ".('"+ vars.get(i) +"')=" + empty + ";";
    		}
    		return cmd;
    	}
    	
    	return null;
    }
    
    public static String getInputVariableAdditionalInformationCommand(String type, List<String> vars, List<String> cols) {
    	if (type.equals("dataset")) {
    		String cmd = "set(" + Matlab.INPUT_VARIABLE_NAME + ",";
    		String varCell = "{";
    		String colCell = "{";
    		for (int i = 0; i < vars.size(); i++) {
    			varCell += "'" + vars.get(i) + "' ";
    			colCell += "'" + cols.get(i) + "' ";
    		}
    		varCell += "}";
    		colCell += "}";
    		return Matlab.INPUT_VARIABLE_NAME + "=" + cmd + "'VarNames'," + varCell + ",'VarDescription'," + colCell + ");";
    	} else {
    		String cmd = Matlab.COLUMNS_VARIABLE_NAME + "=struct(";
    		String colCell = "{";
    		String varCell = "{";
    		for (int i = 0; i < vars.size(); i++) {
    			colCell += "'" + cols.get(i) + "' ";
    			varCell += "'" + vars.get(i) + "' ";
    		}
    		colCell += "}";
    		varCell += "}";
    		cmd += "'matlab'," + varCell + ",'knime'," + colCell + ");"; // Careful, the field names have to be the same as in hashmaputils.m!
    		return cmd;
    	}
    	
    }
    
    
    public static String getAppendRowCommand(String type, DataRow row, List<String>varNames) {
    	if (type.equals("dataset")) {
    		String cell = "{";
    		for (int i = 0; i < row.getNumCells(); i++) {
    			if (row.getCell(i).getType().equals(StringCell.TYPE))
    				if (row.getCell(i).isMissing())
    					cell += "'' ";
    				else
    					cell += "'" + row.getCell(i) + "' ";
    			else
    				if (row.getCell(i).isMissing())
    					cell += Double.NaN + " ";
    				else
    					cell += row.getCell(i) + " ";
    		}
    		cell += "}";
    		return Matlab.INPUT_VARIABLE_NAME + "=[" + Matlab.INPUT_VARIABLE_NAME + ";cell2dataset("+ cell +", 'ReadVarNames', false)];";
    	}
    	if (type.equals("struct")){
    		String cmd = "";
    		String value;
    		for (int i = 0; i < varNames.size(); i++) {
    			if (row.getCell(i).getType().equals(StringCell.TYPE))
    				if (row.getCell(i).isMissing())
    					value = "{''}";
    				else
    					value = "{'" + row.getCell(i) + "'}";
    			else
    				if (row.getCell(i).isMissing())
    					value = "" + Double.NaN;
    				else
    					value = "" + row.getCell(i);
    			cmd += Matlab.INPUT_VARIABLE_NAME + ".('"+ varNames.get(i) +"')" + "(end+1)=" + value + ";" ;
    		}
    		return cmd;
    	}
    	if (type.equals("map")) {
    		String cmd = "";
    		String var, value;
    		for (int i = 0; i < varNames.size(); i++) {
    			if (row.getCell(i).getType().equals(StringCell.TYPE))
    				if (row.getCell(i).isMissing())
    					value = "'';";
    				else
    					value = "'" + row.getCell(i) + ";";
    			else
    				if (row.getCell(i).isMissing())
    					value = "" + Double.NaN;
    				else
    					value = "" + row.getCell(i);
    			var = Matlab.INPUT_VARIABLE_NAME + "('"+ varNames.get(i) +"')";
    			cmd += var + "=[" + var + " " + value + "];" ;
    		}
    		return cmd;
    	}

    	return null;
    }
    
    
    public static String getOutputVariableNamesCommand(String type) {
    	if (type.equals("dataset"))
    		return "get(" + Matlab.OUTPUT_VARIABLE_NAME + ", 'VarNames');";
    	if (type.equals("map"))
    		return Matlab.OUTPUT_VARIABLE_NAME +".keys();";
    	if (type.equals("struct"))
    		return "fieldnames(" + Matlab.OUTPUT_VARIABLE_NAME + ");";
    				
    	return null;
    }
    
    
    public static String getOutputVariableTypesCommand(String type) {
    	if (type.equals("dataset"))
    		return "cellfun(@(x)class("+ Matlab.OUTPUT_VARIABLE_NAME +".(x)),"+ "get(" + Matlab.OUTPUT_VARIABLE_NAME + ", 'VarNames'),'UniformOutput', false)";;
    	if (type.equals("map"))
    		return "cellfun(@(x)class("+ Matlab.OUTPUT_VARIABLE_NAME +"(x)),"+ Matlab.OUTPUT_VARIABLE_NAME +".keys(),'UniformOutput', false)";
    	if (type.equals("struct"))
    		return "structfun(@(x){class(x)},"+ Matlab.OUTPUT_VARIABLE_NAME +");";
    	
    	return null;
    }
    
    public static String getOutputVariableDescriptionsCommand(String type) {
    	if (type.equals("dataset"))
    		return "get(" + Matlab.OUTPUT_VARIABLE_NAME + ", 'VarNames');"; // Take also the variable names
    	else 
    		return "{" + Matlab.COLUMNS_VARIABLE_NAME + ".knime};";
    }
    
    public static String getOutputVariableNumberOfRows(String type) {
    	if (type.equals("dataset"))
    		return "length(" + Matlab.OUTPUT_VARIABLE_NAME + ");";
    	if (type.equals("map"))
    		return Matlab.OUTPUT_VARIABLE_NAME + ".keys;lenght(" + Matlab.OUTPUT_VARIABLE_NAME + "(ans{1}));";
    	if (type.equals("struct"))
    		return "max(structfun(@(x)length(x), " + Matlab.OUTPUT_VARIABLE_NAME + "));";
    	
    	return null;
    }
    
    public static String getRetrieveOutputRowCommand(String type, int rowNumber, String[] varNames) {
    	if (type.equals("dataset"))
    		return "datasetfun(@(x)x(" + rowNumber + ")," + Matlab.OUTPUT_VARIABLE_NAME + ",'UniformOutput',false);";
    	if (type.equals("map")) { //TODO This approach is highly inefficient. since it puts the entire table in the 'ans' variable before accessing it.
    		String cmd = "{";
    		for (String varName : varNames)
    			cmd += Matlab.OUTPUT_VARIABLE_NAME + "('" + varName + "') ";
			cmd += "};ans(" + rowNumber + ",:);";
    		return cmd;
    	}
    	if (type.equals("struct")) {
    		String cmd = "{";
    		for (String varName : varNames)
    			cmd += Matlab.OUTPUT_VARIABLE_NAME + ".('" + varName + "')("+ rowNumber + ") ";
    		return cmd + "};";
    	}
    	
    	return null;
    }
    
    public static String getRetrieveErrorCommand() {
    	return "{" + Matlab.ERROR_VARIABLE_NAME + ".identifier " + Matlab.ERROR_VARIABLE_NAME + ".message}"; 
    }
    
    
    public static void checkForSnippetErrors(MatlabProxy proxy) throws MatlabInvocationException {
    	String[] error = (String[]) proxy.getVariable(MatlabCode.getRetrieveErrorCommand());
		if (error[0].length() > 0)
			throw new RuntimeException(error[0] + ", " + error[1] + " Check your MATLAB code!");
    }
    
}


package de.mpicbg.knime.scripting.matlab.srv.utils;

import java.util.ArrayList;
import java.util.List;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;
import matlabcontrol.MatlabProxy;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;

import de.mpicbg.knime.scripting.matlab.srv.Matlab;
import de.mpicbg.knime.scripting.matlab.srv.MatlabClient;
import de.mpicbg.knime.scripting.matlab.srv.MatlabServer;


/**
 * This class serves to regroup all the MATLAB code generation and manipulations.
 * 
 * On one hand there are constructors for the different cases of MATLAB snippets:
 * - {@link OpenInMatlab}
 * - {@link MatlabPlotNodeModel}
 * - {@link MatlabSnippetNodeModel}
 * The constructor then adds the necessary code to the snippet from the user input
 * like code for loading the table, saving it and the function signature so the code
 * can be put in a file and used as script. (Executing the code as a script instead of passing
 * it directly to {@link MatlabServer#eval} or {@link MatlabProxy#eval(String)} has the
 * advantage that to code can contain line breaks and the MATLAB workspace stays free of 
 * variables created in the input snippet) 
 * The modified script can be retrieved with the {@link this#getSnippet()} method 
 * and the code to execute the script with {@link this#getScriptExecutionCommand(String, boolean, boolean)} 
 * This class does however not take care to of writing the script into a file. The
 * {@link MatlabFileTransfer} class provides the functionality to create a file (which
 * has to be handed to this class to produce the function signature of the script), 
 * writing the code string to the file and to make it available for the local
 * MATLAB application or for a MATLAB application running on a remote host.
 * 
 * On the other hand there are static methods that yield MATLAB code for a specific
 * operation. These provide an easy and centralized way for {@link MatlabClient}, {@link MatlabServer}
 * and {@link MatlabTable} to handle the MATLAB code.
 * 
 * Terminology:
 * 	snippet: code provided by the used (node dialog)
 * 	script: code produced by this class that can be written to a file and called as a function
 * 	variable: variable in the MATLAB workspace
 * 	table: is the data which is held by a variable (since the input is a KNIME table)
 * 
 * @author Felix Meyenhofer
 */
public class MatlabCode {
	
	/** MATLAB code that with added commands to the input code for a specific task */
	private String script = "";
	
	
	/**
	 * Constructor to produce the MATLAB code for a
	 * snippet node using workspace push data transfer.
	 * 
	 * @param code
	 * @param matlabType
	 * @param snippetPath
	 * @throws Exception
	 */
	public MatlabCode(String code, String matlabType, String snippetPath) throws Exception {
		String script = code;
		script = addErrorHandlingCode(script);
		script = addFunctionSignature(script, FilenameUtils.getBaseName(snippetPath), true, true);
		this.script = script;
	}
	
	/**
	 * Constructor to produce the MATLAB code for a 
	 * snippet node using file-based data transfer
	 * 
	 * @param code
	 * @param matlabType
	 * @param parserPath
	 * @param snippetPath
	 * @param tablePath
	 * @throws Exception
	 */
	public MatlabCode(String code, String matlabType, String parserPath, String snippetPath, String tablePath) throws Exception {
		String script = "";
		script = addLoadCode(code, matlabType, parserPath, tablePath);
		script = addSaveCode(script, parserPath, tablePath);
		script = addErrorHandlingCode(script);
		script = addFunctionSignature(script, FilenameUtils.getBaseName(snippetPath), false, true);
		this.script = script;
	}
	
	/**
	 * Constructor to produce the MATLAB code for a
	 * plot node snippet using file-based data transfer.
	 * 
	 * @param code
	 * @param matlabType
	 * @param parserPath
	 * @param snippetPath
	 * @param tablePath
	 * @param plotPath
	 * @param width
	 * @param height
	 * @throws Exception
	 */
	public MatlabCode(String code, String matlabType, String parserPath, String snippetPath, String tablePath, String plotPath, int width, int height) throws Exception {
		String script = "";
		script = addLoadCode(code, matlabType, parserPath, tablePath);
		script = addPlotCode(script, width, height, plotPath);
		script = addErrorHandlingCode(script);
		script = addFunctionSignature(script, FilenameUtils.getBaseName(snippetPath), false, false);
		this.script = script;
	}
	
	/**
	 * Constructor to produce the MATLAB code for a
	 * plot node snippet running using workspace-push based data transfer.
	 * 
	 * @param code
	 * @param matlabType
	 * @param plotPath
	 * @param width
	 * @param height
	 */
	public MatlabCode(String code, String matlabType, String snippetPath, String plotPath, int width, int height) {
		String script = code;
		script = addPlotCode(script, width, height, plotPath);
		script = addErrorHandlingCode(script);
		script = addFunctionSignature(script, FilenameUtils.getBaseName(snippetPath), true, false);
		this.script = script;
	}
	
	
	/**
	 * Get the modified snippet (depending on the on constructor
	 * bits of code are added) 
	 * 
	 * @return
	 */
	public String getScript(){
		return this.script;
	}
	
	/**
     * This method checks if the MATLAB snippet produced any errors
     * and throws a runtime exception that shows in the KNIME console.
     * This is ensures that the user has more specific error information
     * concerning the MATLAB snippet instead of a InternalMatlabException.
     * 
     * This functionality needs the {@link this#addErrorHandlingCode(String)}
     * to be called during the preparation of the snippet.
     */
    public static void checkForScriptErrors(MatlabOperations controller) throws MatlabInvocationException {
    	String[] error = (String[]) controller.getVariable(MatlabCode.getRetrieveErrorCommand());
		if (error[0].length() > 0)
			throw new RuntimeException(error[0] + ", " + error[1] + " Check your MATLAB code!");
    }
	
	/**
	 * Wrap the entire snippet in a try-catch clause to for error handling
	 * 
	 * @param code
	 * @return
	 */
	private String addErrorHandlingCode(String code) {
		return "\ntry\n" + code + "\ncatch " + Matlab.ERROR_VARIABLE_NAME + ";\nend\n";
	}
	
	/**
	 * Add the code to make a function definition out of a MATLAB script
	 * 
	 * @param code
	 * @param functionName
	 * @param hasInput
	 * @param hasOutput
	 * @return
	 */
	private String addFunctionSignature(String code, String functionName, boolean hasInput, boolean hasOutput) {
		return "function " + createFunctionSignature(functionName, hasInput, hasOutput) + "\n" + 
				Matlab.ERROR_VARIABLE_NAME + "=struct('identifier', '', 'message', '');\n" + 
				code;
	}
	
	/**
	 * Create a function signature so the snippet can be packed in a m-file and called as a
	 * function
	 * 
	 * @param functionName
	 * @param hasInput
	 * @param hasOutput
	 * @return
	 */
	private String createFunctionSignature(String functionName, boolean hasInput, boolean hasOutput) {
		String signature = "";
		if (hasOutput)
			signature += "[" + Matlab.OUTPUT_VARIABLE_NAME + "," + Matlab.ERROR_VARIABLE_NAME + "]" +"=" + functionName;
		else
			signature += Matlab.ERROR_VARIABLE_NAME + "=" + functionName;
		
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
	 * @param code
	 * @param plotWidth
	 * @param plotHeight
	 * @param plotPath
	 * @return
	 */
	private String addPlotCode(String code, Integer plotWidth, Integer plotHeight, String plotPath) {
    	return "figureHandle = figure('visible', 'off', 'units', 'pixels', 'position', [0, 0, " + plotWidth + ", " + plotHeight + "]);\n" +
        		"set(gcf,'PaperPositionMode','auto');\n" +
        		code + "\n" +
        		"print(figureHandle, '-dpng', '" + plotPath + "');\n" + 
        		Matlab.OUTPUT_VARIABLE_NAME + "=[];";							// so it conforms with the function signature
	}
	
	/**
	 * Add the code needed to load the object dump of the hash-map 
	 * (KNIME table)
	 * 
	 * @param code
	 * @param scriptPath
	 * @param tablePath
	 * @return
	 * @throws Exception
	 */
	private String addLoadCode(String code, String matlabType, String scriptPath, String tablePath) throws Exception {
		
		String matlabPath = FilenameUtils.getFullPath(scriptPath); 
		String functionName = FilenameUtils.getBaseName(scriptPath);
		
		return "cd " + matlabPath + ";\n" + 
				"[" + Matlab.INPUT_VARIABLE_NAME +"," + Matlab.COLUMNS_VARIABLE_NAME + "]=" + 
				functionName + "('" + tablePath + "','" + matlabType + "');\n" +
				code;
	}
	
	/**
	 * Add the code to save the data in {@link Matlab#OUTPUT_VARIABLE_NAME}
	 * to a binary file.
	 * 
	 * @param code
	 * @param scriptPath
	 * @param tablePath
	 * @return
	 */
	private String addSaveCode(String code, String scriptPath, String tablePath) {
		String matlabPath = FilenameUtils.getFullPath(scriptPath);
		String functionName = FilenameUtils.getBaseName(scriptPath);
		return code + "\n" +
				"cd " + matlabPath + ";\n" +
				functionName + "('" + tablePath + "', " + Matlab.OUTPUT_VARIABLE_NAME +");";
	}
	
	/**
	 * Get the command to execute the script produced with this class.
	 * This requires that it was instantiated with a (empty) file where
	 * the code later is written to.
	 * 
	 * @param snippetPath
	 * @param hasInput
	 * @param hasOutput
	 * @return
	 */
	public String getScriptExecutionCommand(String snippetPath, boolean hasInput, boolean hasOutput) {
		String path = FilenameUtils.getFullPath(snippetPath);
		String fun = FilenameUtils.getBaseName(snippetPath);
		return "cd " + path + ";" + createFunctionSignature(fun, hasInput, hasOutput) + ";";
	}
    
	/**
	 * Generate variable names from the KNIME table column names.
	 * Some characters are not supported in certain MATLAB type 
	 * and need to be removed.
	 * 
	 * @param type
	 * @param colNames
	 * @return
	 */
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
    
    /**
     * Get the MATLAB command to instantiate the MATLAB input variable
     * according to a given MATLAB variable type.
     * 
     * @param type
     * @param vars
     * @param types
     * @return
     */
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
    
    /**
     * Get additional information for the table. Depending on the type
     * this information is stored in the dataset or in the additional 
     * MATLAB variable {@link Matlab#COLUMNS_VARIABLE_NAME}.
     * 
     * @param type
     * @param vars
     * @param cols
     * @return
     */
    public static String getInputColumnAdditionalInformationCommand(String type, List<String> vars, List<String> cols) {
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
    
    /**
     * Get the MATLAB code to append a row to the table, given 
     * a MATLAB variable type
     * 
     * @param type
     * @param row
     * @param varNames
     * @return
     */
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
    					value = "'" + row.getCell(i) + "'";
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
    
    /**
     * Get the column names of the output table produced by the MATLAB
     * snippet
     * 
     * @param type
     * @return
     */
    public static String getOutputColumnNamesCommand(String type) {
    	if (type.equals("dataset"))
    		return "get(" + Matlab.OUTPUT_VARIABLE_NAME + ", 'VarNames');";
    	if (type.equals("map"))
    		return Matlab.OUTPUT_VARIABLE_NAME +".keys();";
    	if (type.equals("struct"))
    		return "fieldnames(" + Matlab.OUTPUT_VARIABLE_NAME + ");";
    				
    	return null;
    }
    
    /**
     * Get the column types of the output table produced by the MATLAB
     * snippet.
     * 
     * @param type
     * @return
     */
    public static String getOutputColumnTypesCommand(String type) {
    	if (type.equals("dataset"))
    		return "cellfun(@(x)class("+ Matlab.OUTPUT_VARIABLE_NAME +".(x)),"+ "get(" + Matlab.OUTPUT_VARIABLE_NAME + ", 'VarNames'),'UniformOutput', false)";;
    	if (type.equals("map"))
    		return "cellfun(@(x)class("+ Matlab.OUTPUT_VARIABLE_NAME +"(x)),"+ Matlab.OUTPUT_VARIABLE_NAME +".keys(),'UniformOutput', false)";
    	if (type.equals("struct"))
    		return "structfun(@(x){class(x)},"+ Matlab.OUTPUT_VARIABLE_NAME +");";
    	
    	return null;
    }
    
    /**
     * Get the column descriptions of the output table produced
     * by the MATLAB snippet.
     * The column descriptions can be used to have column names
     * in the KNIME table with special characters.
     * 
     * @param type
     * @return
     */
    public static String getOutputColumnDescriptionsCommand(String type) {
    	if (type.equals("dataset"))
    		return "get(" + Matlab.OUTPUT_VARIABLE_NAME + ", 'VarNames');"; // Take also the variable names
    	else 
    		return "{" + Matlab.COLUMNS_VARIABLE_NAME + ".knime};";
    }
    
    /**
     * Get the number of rows of the output table produced by the
     * MATLAB snippet
     * 
     * @param type
     * @return
     */
    public static String getOutputTableNumberOfRowsCommand(String type) {
    	if (type.equals("dataset"))
    		return "length(" + Matlab.OUTPUT_VARIABLE_NAME + ");";
    	if (type.equals("map"))
    		return Matlab.OUTPUT_VARIABLE_NAME + ".keys;lenght(" + Matlab.OUTPUT_VARIABLE_NAME + "(ans{1}));";
    	if (type.equals("struct"))
    		return "max(structfun(@(x)length(x), " + Matlab.OUTPUT_VARIABLE_NAME + "));";
    	
    	return null;
    }
    
    /**
     * Get the MATLAB command to retrieve an row of the output table
     * produced by the MATLAB snippet.
     * 
     * @param type
     * @param rowNumber
     * @param varNames
     * @return
     */
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
    
    /**
     * Get the code to retrieve the variable containing the error
     * messages produced by the MATLAB snippet.
     * This variable is set by the code produced by {@link this#addErrorHandlingCode(String)}
     * 
     * @return
     */
    public static String getRetrieveErrorCommand() {
    	return "{" + Matlab.ERROR_VARIABLE_NAME + ".identifier " + Matlab.ERROR_VARIABLE_NAME + ".message}"; 
    }
    
    /**
     * Get the code to display the thread information in the
     * MATLAB command window.
     * 
     * @param threadNumber
     * @return
     */
    public static String getThreadInfoCommand(int threadNumber) {
    	return "disp(' ');disp('Thread "+ threadNumber +":');";
    }
    
    /**
     * Get the code to clear the relevant variables in the MATLAB
     * workspace.
     * 
     * @return
     */
    public static String getClearWorkspaceCommand() {
    	return "clear " + Matlab.INPUT_VARIABLE_NAME + " " + Matlab.COLUMNS_VARIABLE_NAME + " " + Matlab.OUTPUT_VARIABLE_NAME + " " + Matlab.ERROR_VARIABLE_NAME;
    }
    
    /**
     * Get the command to make the KNIME table data available
     * in the MATLAB workspace.
     * 
     * @param matlabType
     * @param parserPath
     * @param tablePath
     * @return
     */
    public static String getOpenInMatlabCommand(String matlabType, String parserPath, String tablePath) {
    	String matlabPath = FilenameUtils.getFullPath(parserPath);
    	String functionName = FilenameUtils.getBaseName(parserPath);
    			
    	return "cd " + matlabPath + ";" + 
				"[" + Matlab.INPUT_VARIABLE_NAME +"," + Matlab.COLUMNS_VARIABLE_NAME + "]=" + 
				functionName + "('" + tablePath + "','" + matlabType + "');" +
				getOpenMessage(matlabType);
    }
    
    /**
     * Get the code to display the message from a plot node.
     * {@link MatlabPlotNodeModel}
     * 
     * @param changedInputVariables
     * @return
     */
    public static String getPlotNodeMessage(boolean changedInputVariables){
    	String msg = "disp('created plot";
    	if (changedInputVariables)
    		msg += " and updated " + Matlab.INPUT_VARIABLE_NAME + ", " + Matlab.COLUMNS_VARIABLE_NAME + " ')";
    	return  msg + ".');";
    }
    
    /**
     * Get the code do display the message from a snippet node
     * {@link MatlabSnippetNodeModel}
     * 
     * @param changedInputVariable
     * @return
     */
    public static String getSnippetNodeMessage(boolean changedInputVariable) {
    	String msg = "disp('exectuted snippet and updated " + Matlab.OUTPUT_VARIABLE_NAME;
    	if (changedInputVariable)
    		msg += ", " + Matlab.INPUT_VARIABLE_NAME + " and " + Matlab.COLUMNS_VARIABLE_NAME;
    	return msg + ".');";
    }
    
    /**
	 * Add a message to be displayed in the MATLAB command window informing the user
	 * on what happened after the execution of the {@link OpenInMatlab} node
	 *  
	 * @param code MATLAB snippet
	 * @return Modified code
	 */
	public static String getOpenMessage(String matlabType) {
		return "disp('The data is available as the following variables in the Workspace:');" +
				"disp('kIn :  " + matlabType + " containing the KNIME table.');" + 
				"disp('columnNames: structure containing column header information.');" +
				"disp('             Depending on the MATLAB data type strings used KNIME might need slight modification!');" +
				"disp('To reload the KNIME table simply re-execute the OpenInMatlab node.');";
	}
    
}


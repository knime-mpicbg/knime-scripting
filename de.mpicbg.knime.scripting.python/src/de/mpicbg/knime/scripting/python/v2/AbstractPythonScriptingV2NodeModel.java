package de.mpicbg.knime.scripting.python.v2;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

import com.opencsv.CSVWriter;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.Python;


public abstract class AbstractPythonScriptingV2NodeModel extends AbstractScriptingNodeModel {
	
	public static final String PY_INVAR_BASE_NAME = "kIn";
	public static final String PY_OUTVAR_BASE_NAME = "pyOut";
	public static final String PY_SCRIPTVAR_BASE_NAME = "pyScript";
	
	public static final String CFG_SCRIPT_DFT = "pyOut = kIn";
	
    // Temp files for reading/writing the table and the script
    //protected PythonTempFile kInFile;
    //protected PythonTempFile pyOutFile;
    //protected PythonTempFile scriptFile;

    protected Python python;

    protected IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    /**
	 * @param inPorts
	 * @param outPorts
	 * @param rColumnSupport
	 */
	public AbstractPythonScriptingV2NodeModel(PortType[] inPorts, PortType[] outPorts, PythonColumnSupport rColumnSupport) {
		super(inPorts, outPorts, rColumnSupport);
	}

	/**
	 * 
	 * @param inPorts
	 * @param outPorts
	 */
	public AbstractPythonScriptingV2NodeModel(PortType[] inPorts, PortType[] outPorts) {
		super(inPorts, outPorts, new PythonColumnSupport());
	}

	/**
	 * constructor with node configuration object
	 * @param nodeModelConfig
	 */
	public AbstractPythonScriptingV2NodeModel(ScriptingModelConfig nodeModelConfig) {
		super(nodeModelConfig);
	}
	
	/**
	 * main method to push available input to R
	 * NOTE: method does not close the connection in case of exceptions
	 * 
	 * @param inData
	 * @param exec
	 * @throws KnimeScriptingException
	 * @throws CanceledExecutionException
	 */
	protected void pushInputToPython(PortObject[] inData, ExecutionContext exec) 
			throws KnimeScriptingException, CanceledExecutionException {
		
		//ScriptingModelConfig cfg = getNodeCfg();
		
		exec.setMessage("Transfer to Python");
		ExecutionMonitor transferToExec = exec.createSubProgress(1.0/2);
		
		// assign ports to Python variable names
		Map<String, BufferedDataTable> inPorts = createPortMapping(inData);
		Map<String, File> tempFiles = createTempFiles(inPorts);
		
		//int nInTables = inPorts.size();
		
		// push all KNIME data tables
		for(String in : inPorts.keySet()) {
			transferToExec.setMessage("Push table");
			BufferedDataTable inTable = inPorts.get(in);
			try {
				pushTableToPython((BufferedDataTable) inTable, in, tempFiles.get(in), transferToExec.createSubProgress(1.0/inPorts.size()));
			} catch (KnimeScriptingException kse) {
				deleteTempFiles(tempFiles);
				throw kse;
			}
		}
	}
	
	private void deleteTempFiles(Map<String, File> tempFiles) {
		
		for(File tempFile : tempFiles.values()) {
			if(tempFile != null)
				try {
					Files.deleteIfExists(tempFile.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	/**
	 * push one KNIME table and its properties to R
	 * @param inTable
	 * @param varName
	 * @param tempFile 
	 * @param exec
	 * @param chunkInSize 
	 * @throws CanceledExecutionException 
	 * @throws REXPMismatchException 
	 * @throws RserveException 
	 * @throws KnimeScriptingException 
	 */
	private void pushTableToPython(BufferedDataTable inTable, String varName, File tempFile, ExecutionMonitor exec) 
			throws CanceledExecutionException, KnimeScriptingException {

		DataTableSpec inSpec = inTable.getSpec();
		
		python = new LocalPythonClient();
		
		Map<String, String> supportedColumns = new LinkedHashMap<String, String>();
		supportedColumns.put("Row ID", "INDEX");
		
		Map<String, Integer> columnsIndicees = new LinkedHashMap<String, Integer>();
		columnsIndicees.put("Row ID", -1);
        
        /*
         * go through columns, if supported type, add their name and python type to the lists
         */
        for(int i = 0; i < inSpec.getNumColumns(); i++) {
        	DataColumnSpec cSpec = inSpec.getColumnSpec(i);
        	DataType dType = cSpec.getType();
        	String cName = cSpec.getName();
        	
        	if(dType.getCellClass().equals(BooleanCell.class)) {
        		supportedColumns.put(cName, "bool");   
        		columnsIndicees.put(cName, i);
        	}
        	if(dType.getCellClass().equals(IntCell.class) || dType.getCellClass().equals(LongCell.class)) {
        		supportedColumns.put(cName, "int");  
        		columnsIndicees.put(cName, i);
        	}
        	if(dType.getCellClass().equals(DoubleCell.class)) {
        		supportedColumns.put(cName, "float"); 
        		columnsIndicees.put(cName, i);
        	}
        	if(dType.getCellClass().equals(LocalTimeCell.class) ||
        		dType.getCellClass().equals(LocalDateCell.class) ||
        		dType.getCellClass().equals(LocalDateTimeCell.class) ||
        		dType.getCellClass().equals(ZonedDateTimeCell.class)) 
        	{
        		supportedColumns.put(cName, "datetime64[ns]");  
        		columnsIndicees.put(cName, i);
        	}
        	if(dType.getCellClass().equals(StringCell.class)) {
        		supportedColumns.put(cName, "object");  
        		columnsIndicees.put(cName, i);
        	}	
        }
        
		Writer writer;
		try {
			writer = Files.newBufferedWriter(tempFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new KnimeScriptingException("Failed to write table to CSV: " + e.getMessage());
		}

        CSVWriter csvWriter = new CSVWriter(writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        
        String[] columnNames = supportedColumns.keySet().toArray(new String[supportedColumns.size()]);
        String[] columnTypes = supportedColumns.values().toArray(new String[supportedColumns.size()]);
        
        // write column names (at least 'Row ID')
        csvWriter.writeNext(columnNames, false);
        csvWriter.writeNext(columnTypes, false);
        
        for(DataRow row : inTable) {
        	List<String> columnValues = new LinkedList<String>();
        	      	
        	for(String col : columnsIndicees.keySet()) {
        		int idx = (Integer)columnsIndicees.get(col);
        		if( idx == -1)
        			columnValues.add(row.getKey().getString());
        		else {
        			DataCell cell = row.getCell(idx);
        			DataType dType = cell.getType();

        			if(cell.isMissing()) {
        				columnValues.add(null);
        			} else {
        				if(dType.getCellClass().equals(BooleanCell.class)) {
        					boolean val = ((BooleanValue) cell).getBooleanValue();
        					columnValues.add(val ? "1" : "0");
        				}
        				if(dType.getCellClass().equals(IntCell.class) || dType.getCellClass().equals(LongCell.class)) {
        					int val = ((IntValue) cell).getIntValue();
        					columnValues.add(Integer.toString(val));
        				}
        				if(dType.getCellClass().equals(DoubleCell.class)) {
        					double val = ((DoubleValue) cell).getDoubleValue();
        					columnValues.add(Double.toString(val));
        				}
        				if(dType.getCellClass().equals(StringCell.class)) {
        					String val = ((StringValue) cell).getStringValue();
        					columnValues.add(val);
        				}
        			}
        		}
        	}
        	
        	String[] values = columnValues.toArray(new String[columnValues.size()]);
        	csvWriter.writeNext(values, false);
        	
        	exec.checkCanceled();
        }
        
        try {
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new KnimeScriptingException("Failed to write table to CSV: " + e.getMessage());
		}
	}
	
	/**
	 * The map stores the R-names as key and the BDT as value; in case of RPorts, the name is 'generic'
	 * @param inObjects
	 * @return
	 * @throws KnimeScriptingException thrown if more than one generic input ports or unsupported port type
	 */
	public Map<String, BufferedDataTable> createPortMapping(PortObject[] inObjects) throws KnimeScriptingException {

		Map<String, BufferedDataTable> portVarMapping = new TreeMap<String, BufferedDataTable>();

		// number of non-null input port objects; (optional) input ports might be null if not connected
		int nTableInputs = 0;
		for (PortObject inport : inObjects) {
			if (inport != null) {
				if(inport instanceof BufferedDataTable) nTableInputs++;
				else
					throw new KnimeScriptingException("Implementation error: PortType " 
							+ inport.getClass().getSimpleName() + "is not yet supported");
			}
		}

		// create naming of input variables for Python; e.g. kIn, or kIn1
		// map port objects to variable name
		int i = 0;
		for (PortObject inport : inObjects) {

			if(inport != null) {
				PortType pType = getInPortType(i);
	
				if(pType.equals(BufferedDataTable.TYPE)) {
					String variableName = PY_INVAR_BASE_NAME + (nTableInputs > 1 ? (i + 1) : "");
					portVarMapping.put(variableName, (BufferedDataTable) inport);
					i++;
				}
			}
		}

		return portVarMapping;
	}
	
    /**
     * Create necessary temp files
     * @param inPorts 
     * @return 
     * @throws KnimeScriptingException 
     */
    protected Map<String, File> createTempFiles(Map<String, BufferedDataTable> inPorts) throws KnimeScriptingException {
        
    	Map<String, File> tempFileMapping = new LinkedHashMap<String, File>();
    	
    	try {
	    	// Create a new set
	    	for(String label : inPorts.keySet()) {
	    		File tempFile = File.createTempFile(label + "_knime2python", ".csv");
	    		tempFileMapping.put(label, tempFile);
	    	}
	
	    	File pyOutFile = File.createTempFile("_python2knime", ".csv");
	    	File scriptFile = File.createTempFile("_analyze", ".py");
	
	    	tempFileMapping.put(PY_OUTVAR_BASE_NAME, pyOutFile);
	    	tempFileMapping.put(PY_SCRIPTVAR_BASE_NAME, scriptFile);
    	} catch (IOException ioe) {
    		throw new KnimeScriptingException("Failed to create temporary files: " + ioe.getMessage());
    	} finally {
    		for(File f : tempFileMapping.values()) {
    			if(f != null)
					try {
						Files.deleteIfExists(f.toPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
    		}
    	}

    	return tempFileMapping;
    }

	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		pushInputToPython(inData, exec);	
		return null;
	}

	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException, CanceledExecutionException {
		// TODO Auto-generated method stub
		
	}
    
    

	/** ============================================================================================= */

 /*   protected void prepareScript(Writer writer, boolean useScript) throws IOException {
        // CSV read/write functions
        //InputStream utilsStream = PythonScripts.class.getResourceAsStream("PythonCSVUtils.py");
    	InputStream utilsStream = getClass().getClassLoader().getResourceAsStream("/de/mpicbg/knime/scripting/python/scripts/PythonCSVUtils.py");


        String readCSVCmd = kInFile != null ? "kIn, columnTypes = read_csv(r\"" + kInFile.getServerPath() + "\", True)" : "";
        String writeCSVCmd = pyOutFile != null ? "write_csv(r\"" + pyOutFile.getServerPath() + "\", pyOut, True)" : "";

        // Write the script file
        writer.write(TemplateUtils.convertStreamToString(utilsStream));
        writer.write("\n" + readCSVCmd + "\n");

        // Insert the user-defined script here
        if(useScript)
        	writer.write("\n" + super.prepareScript() + "\n");

        writer.write("\n" + writeCSVCmd + "\n");

        return;
    }

    @Override
    public String prepareScript() throws RuntimeException {
        try {
            Writer writer = new BufferedWriter(new FileWriter(scriptFile.getClientFile()));
            try {
                prepareScript(writer, true);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        scriptFile.upload();
        return scriptFile.getServerPath();
    }

*/

    /**
     * Delete all temp files if they exist and the node is so configured
     */
 /*   protected void deleteTempFiles() {
        if (kInFile != null) kInFile.delete();
        if (pyOutFile != null) pyOutFile.delete();
        if (scriptFile != null) scriptFile.delete();
    }


    protected void openInPython(PortObject[] inData, ExecutionContext exec, NodeLogger logger) throws KnimeScriptingException {
    	IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    	//      boolean local = preferences.getBoolean(PythonPreferenceInitializer.PYTHON_LOCAL);
    	//      if (!local) throw new RuntimeException("This node can only be used with a local python executable");

    	python = new LocalPythonClient();

    	createTempFiles();
    	pyOutFile = null;

    	// Write data into csv
    	BufferedDataTable[] inTables = castToBDT(inData);
    	logger.info("Writing table to CSV file");
    	PythonTableConverter.convertTableToCSV(exec, inTables[0], kInFile.getClientFile(), logger);

    	// Create and execute script
    	String pythonExecPath = preferences.getString(PythonPreferenceInitializer.PYTHON_EXECUTABLE);

    	// get the full path of the python executable for MacOS
    	String pythonExecPathFull = pythonExecPath;
    	try {
    		if (Utils.isMacOSPlatform()) {
    			Runtime r = Runtime.getRuntime();
    			Process p = r.exec("which " + pythonExecPath);
    			p.waitFor();
    			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    			pythonExecPathFull = reader.readLine();
    		}
    	} catch (Exception e) {
    		logger.error(e);
    	}

    	try {
    		Writer writer = new BufferedWriter(new FileWriter(scriptFile.getClientFile()));
    		try {
    			// Write a shebang to invoke the python interpreter 
    			writer.write("#! " + pythonExecPathFull + " -i\n");
    			prepareScript(writer, false);
    		} finally {
    			writer.close();
    		}

    		scriptFile.getClientFile().setExecutable(true);

    		// Run the script
    		if (Utils.isMacOSPlatform()) {
    			Runtime.getRuntime().exec("open -a Terminal " + " " + scriptFile.getClientPath());
    		} else if (Utils.isWindowsPlatform()) {
    			Runtime.getRuntime().exec(new String[] {
    					"cmd",
    					"/k",
    					"start",
    					pythonExecPath,
    					"-i",
    					"\"" + scriptFile.getClientPath() + "\""
    			});
    		} else logger.error("Unsupported platform");
    		
    		// copy the script in the clipboard
    		String actualScript = super.prepareScript();
            if (!actualScript.isEmpty()) {
                StringSelection data = new StringSelection(actualScript);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(data, data);
            }
    		
    	} catch (Exception e) {
    		throw new KnimeScriptingException("Failed to open in Python\n" + e);
    	}
    }*/
}

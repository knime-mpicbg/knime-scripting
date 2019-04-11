package de.mpicbg.knime.scripting.python.v2;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import de.mpicbg.knime.knutils.FileUtils;
import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;
import de.mpicbg.knime.scripting.python.srv.CommandOutput;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.Python;;

/**
 * abstract model class for all python scripting nodes
 * take care of data transfer methods from/to python
 * 
 * @author Antje Janosch
 *
 */
public abstract class AbstractPythonScriptingV2NodeModel extends AbstractScriptingNodeModel {
	
	/**
	 * constants
	 */
	
	public static final String PY_INVAR_BASE_NAME = "kIn";
	public static final String PY_OUTVAR_BASE_NAME = "pyOut";
	public static final String PY_SCRIPTVAR_BASE_NAME = "pyScript";
	
	public static final String CFG_SCRIPT_DFT = "pyOut = kIn";
	
	/**
	 * temp files and input/output ports
	 */
	
	Map<String, File> m_tempFiles;
	Map<String, PortObject> m_inPorts;
	Map<String, PortObject> m_outPorts;

	// python executor
    protected Python python;

    // python preferences
    protected IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    // supported pandas column types
    // 'object','bool','float','int','datetime64[ns]'
    public static final String PY_TYPE_OBJECT = "object";
    public static final String PY_TYPE_BOOL= "bool";
    public static final String PY_TYPE_FLOAT = "float64";
    public static final String PY_TYPE_INT = "int64";
    public static final String PY_TYPE_DATETIME = "datetime64[ns]";
    public static final String PY_TYPE_INDEX = "INDEX";
    
    // date-time format exported by python
    public static final DateTimeFormatter PY_dateFormatter = initDateTime();
    
    // store sdtout messages from pythin script execution
    private List<String> m_stdOut = new LinkedList<String>();
    
    /**
	 * @param inPorts
	 * @param outPorts
	 * @param columnSupport
	 */
	public AbstractPythonScriptingV2NodeModel(PortType[] inPorts, PortType[] outPorts, PythonColumnSupport columnSupport) {
		super(inPorts, outPorts, columnSupport);
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
	 * init date/time formatter
	 * 
	 * @return {@link DateTimeFormatter}
	 */
	private static DateTimeFormatter initDateTime() {
		DateTimeFormatterBuilder dateBuilder = new DateTimeFormatterBuilder();
		
		dateBuilder.appendValue(ChronoField.YEAR);
		dateBuilder.appendLiteral('-');
		dateBuilder.appendValue(ChronoField.MONTH_OF_YEAR);
		dateBuilder.appendLiteral('-');
		dateBuilder.appendValue(ChronoField.DAY_OF_MONTH);
		dateBuilder.appendLiteral('_');
		dateBuilder.appendValue(ChronoField.HOUR_OF_DAY);
		dateBuilder.appendLiteral(':');
		dateBuilder.appendValue(ChronoField.MINUTE_OF_HOUR);
		dateBuilder.appendLiteral(':');
		dateBuilder.appendValue(ChronoField.SECOND_OF_MINUTE);
		
		return dateBuilder.toFormatter();
	}

	
	/**
	 * main method to push available input to Python
	 * 
	 * @param inData
	 * @param exec
	 * @throws KnimeScriptingException
	 * @throws CanceledExecutionException
	 */
	protected void pushInputToPython(PortObject[] inData, ExecutionContext exec) 
			throws KnimeScriptingException, CanceledExecutionException {
		
		exec.setMessage("Transfer to Python");
		ExecutionMonitor subExec = exec.createSubProgress(1.0 / 2);
		
		// assign ports to Python variable names and create temporary files
		createInPortMapping(inData);
		createOutPortMapping();	
		createTempFiles();
		
		int nIn = m_inPorts.size();
		
		// push all KNIME data tables
		for(String in : m_inPorts.keySet()) {
			
			ExecutionMonitor transferExec = subExec.createSubProgress(1.0 / nIn);			
			transferExec.setMessage("Push table " + in);
			
			BufferedDataTable inTable = (BufferedDataTable) m_inPorts.get(in);
			try {
				pushTableToPython((BufferedDataTable) inTable, in, m_tempFiles.get(in), transferExec);
				m_inPorts.replace(in, null);	// delete reference to input table after successful transfer
			} catch (KnimeScriptingException kse) {
				deleteTempFiles();
				throw kse;
			}
		}
		
	}
	
	/**
	 * write one KNIME table to temporary CSV file
	 * @param inTable
	 * @param varName
	 * @param tempFile
	 * @param exec
	 * @throws CanceledExecutionException
	 * @throws KnimeScriptingException
	 */
	private void pushTableToPython(BufferedDataTable inTable, String varName, File tempFile, 
			ExecutionMonitor exec) 
			throws CanceledExecutionException, KnimeScriptingException {
	
		DataTableSpec inSpec = inTable.getSpec();
		
		python = new LocalPythonClient();
		
		Map<String, String> supportedColumns = new LinkedHashMap<String, String>();
		supportedColumns.put("Row ID", PY_TYPE_INDEX);
		
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
	    		supportedColumns.put(cName, PY_TYPE_BOOL);   
	    		columnsIndicees.put(cName, i);
	    	}
	    	if(dType.getCellClass().equals(IntCell.class) || dType.getCellClass().equals(LongCell.class)) {
	    		supportedColumns.put(cName, PY_TYPE_INT);  
	    		columnsIndicees.put(cName, i);
	    	}
	    	if(dType.getCellClass().equals(DoubleCell.class)) {
	    		supportedColumns.put(cName, PY_TYPE_FLOAT); 
	    		columnsIndicees.put(cName, i);
	    	}
	    	if(dType.getCellClass().equals(LocalTimeCell.class) ||
	    		dType.getCellClass().equals(LocalDateCell.class) ||
	    		dType.getCellClass().equals(LocalDateTimeCell.class)) 
	    	{
	    		supportedColumns.put(cName, PY_TYPE_DATETIME);  
	    		columnsIndicees.put(cName, i);
	    	}
	    	if(dType.getCellClass().equals(StringCell.class)) {
	    		supportedColumns.put(cName, PY_TYPE_OBJECT);  
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
	    
	    long nRows = inTable.size();
	    
	    int currentRowIdx = 1;
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
	    					continue;
	    				}
	    				if(dType.isCompatible(StringValue.class)) {
	    					String val = ((StringValue) cell).getStringValue();
	    					columnValues.add(val);
	    				}
	    			}
	    		}
	    	}
	    	
	    	String[] values = columnValues.toArray(new String[columnValues.size()]);
	    	csvWriter.writeNext(values, false);
	    	
	    	exec.setProgress((double)currentRowIdx / (double)nRows, "(Row " + currentRowIdx +  "/ " + nRows + ")");
	    	exec.checkCanceled();
	    	
	    	currentRowIdx++;
	    }
	    
	    try {
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new KnimeScriptingException("Failed to write table to CSV: " + e.getMessage());
		}
	}


	/**
	 * main method to retrieve data from Python to provide the output ports
	 * 
	 * @param exec
	 * @return	array of port objects
	 * @throws KnimeScriptingException
	 */
	protected PortObject[] pullOutputFromPython(ExecutionContext exec) 
			throws KnimeScriptingException, CanceledExecutionException {
		
		exec.setMessage("Python snippet finished - pull data from Python");
		ExecutionMonitor transferToExec = exec.createSubProgress(1.0/2);
		
		int nOut = m_outPorts.size();
		
		// pull all python data tables
		for(String out : m_outPorts.keySet()) {
			transferToExec.setMessage("Pull table");
			try {
				double subProgress = 1.0/nOut;
				PortObject outTable = null;
				try {
					outTable = pullTableFromPython(out, exec, subProgress);
				} catch(IOException ioe) {
					throw new KnimeScriptingException("Failed to read table: " + ioe.getMessage());
				}				
				m_outPorts.replace(out, outTable);	// delete reference to input table after successful transfer
			} finally {
				deleteTempFiles();
			}
		}
		
		PortObject[] ports = new PortObject[m_outPorts.size()];
		return m_outPorts.values().toArray(ports);
	}


	/**
	 * retrieve single result table from Python 
	 * 
	 * @param out				label of python output (e.g. pyOut)
	 * @param exec			
	 * @param subProgress
	 * @return
	 * @throws CanceledExecutionException
	 * @throws IOException
	 * @throws KnimeScriptingException
	 */
	private PortObject pullTableFromPython(String out, ExecutionContext exec, double subProgress) throws CanceledExecutionException, IOException, KnimeScriptingException {
		
		ExecutionMonitor execM = exec.createSubProgress(subProgress);
		execM.setMessage("retrieve " + out);
		
		File tempFile = m_tempFiles.get(out);
		
		assert tempFile != null;
		
		// read number of lines (approximate, as line breaks may occur within quotes)
		// just used to provide progress estimate
		int nLines = -1;
	
		try (FileInputStream fis = new FileInputStream(tempFile)) {
			nLines = FileUtils.countLines(fis);
		}
	
		// parse CSV
		CSVParser parser = new CSVParserBuilder()
				.withEscapeChar(CSVParser.DEFAULT_ESCAPE_CHARACTER)
				.withSeparator(CSVParser.DEFAULT_SEPARATOR)
				.withQuoteChar(CSVParser.DEFAULT_QUOTE_CHARACTER)
				.withStrictQuotes(false)
				.build();
		BufferedDataContainer bdc = null;
		
		try (BufferedReader br = Files.newBufferedReader(tempFile.toPath(), StandardCharsets.UTF_8);
				CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser)
						.build()) {
			
			String[] columnNames = null;
			Map<String, DataType> columns = new LinkedHashMap<String, DataType>();
	
			// go line by line
			// 1. line = column names
			// 2. line = column types
			int lineCount = 0;
			for(String[] line : reader) {
				if(lineCount == 0) {
					columnNames = line;
					lineCount ++;
					continue;
				}
				if(lineCount == 1) {
					int i = 0;
					for(String cType : line) {
						if(i == 0) { i++; continue; }
						columns.put(columnNames[i], getKnimeDataType(cType));
						i++;
					}
					DataTableSpec tSpec = createDataTableSpec(columns);
					bdc = exec.createDataContainer(tSpec);
					lineCount ++;
					continue;
				}
	
				String rowID = line[0];
				List<DataCell> dataCells = new LinkedList<DataCell>();
	
				int i= 1;
				for(String col : columns.keySet()) {
					String value = line[i];
					if(value.isEmpty())
						dataCells.add(DataType.getMissingCell());
					else {
						DataType dType = columns.get(col);
						DataCell  addCell = createCell(value, dType);
						dataCells.add(addCell);
					}
					i++;
				}
				DataCell[] cellArray = new DataCell[dataCells.size()];
				DefaultRow row = new DefaultRow(new RowKey(rowID), dataCells.toArray(cellArray));
	
				bdc.addRowToTable(row);
	
				lineCount++;
				
				execM.checkCanceled();
				execM.setMessage(lineCount + " line(s) read");
				execM.setProgress((double)lineCount / (double)nLines);
			}
		}
		
		bdc.close();
		
		return bdc.getTable();
	}

	/**
	 * execute python script and make sure that temp files are deleted if something goes wrong
	 * 
	 * @param exec
	 * @throws KnimeScriptingException
	 */
	protected void runScript(ExecutionMonitor exec) throws KnimeScriptingException {
			
		try {
			runScriptImpl(exec);
		} catch (KnimeScriptingException kse) {
			deleteTempFiles();
			throw kse;
		}
		
		if(!m_stdOut.isEmpty()) {
			String warningString = String.join("\n", m_stdOut);
			this.setWarningMessage(warningString);
		}
	}


	/**
	 * main method to run the script (after pushing data and before pulling result data)
	 * @param exec
	 * @throws KnimeScriptingException
	 */
	private void runScriptImpl(ExecutionMonitor exec) throws KnimeScriptingException {
	
		exec.setMessage("Evaluate Python-script (cannot be cancelled)");
	
		IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();
	
		// prepare script
		File scriptFile = m_tempFiles.get(PY_SCRIPTVAR_BASE_NAME);
		try (Writer writer = new BufferedWriter(new FileWriter(scriptFile))) {
			prepareScript(scriptFile, true);
		} catch(IOException ioe) {
			throw new KnimeScriptingException("Failed to prepare script: " + ioe.getMessage());
		} 
	
	
		// run script
	
		// get executable
		String pythonExecPath = getPythonExecutable(preferences);
		if(pythonExecPath.isEmpty())
			throw new KnimeScriptingException("Path to python executable is not set. Please configure under Preferences > KNIME > Python Scripting.");
	
		CommandOutput output;
		try {
			output = python.executeCommand(new String[]{pythonExecPath, scriptFile.getCanonicalPath()});
		} catch (IOException ioe) {
			throw new KnimeScriptingException("Failed to load script file: " + ioe);
		} catch (RuntimeException re) {
			throw new KnimeScriptingException("Failed to execute Python: " + re);
		}
	
		if(output.hasStandardOutput()) {
			m_stdOut = output.getStandardOutput();
		}
		if(output.hasErrorOutput())
			throw new KnimeScriptingException("Error in processing: " + String.join("\n", output.getErrorOutput()));
	
	}
	
	/**
	 * load template with methods to read data from KNIME and write result back as CSV <br/>
	 * extend by call these methods with the temporary files
	 * 
	 * @param scriptFile
	 * @param useScript		if false, final script only allows to read data from knime, <br/>
	 * no user script append or result CSV written
	 * 
	 * @throws KnimeScriptingException
	 */
	protected void prepareScript(File scriptFile,boolean useScript) throws KnimeScriptingException {
		// CSV read/write functions
		
		// (1) write template script to script file
		try (InputStream utilsStream = getClass().getClassLoader().getResourceAsStream("/de/mpicbg/knime/scripting/python/scripts/PythonCSVUtils2.py")) {
			String pyScriptString = FileUtils.readFromRessource(utilsStream);
			Files.write(scriptFile.toPath(), pyScriptString.getBytes(), StandardOpenOption.APPEND);
		}catch(IOException ioe) {
			throw new KnimeScriptingException("Failed to write script file: " + ioe.getMessage());
		}
	
		// (2) add read-calls to script file
		for(String inLabel : m_inPorts.keySet()) {
			File f = m_tempFiles.get(inLabel);
			String errorMessage = null;
			if(f != null) {
				try {
					String readCSVCmd = "\n\n" + inLabel + " = read_csv(r\"" + f.getCanonicalPath() + "\")\n\n";
					Files.write(scriptFile.toPath(), readCSVCmd.getBytes(), StandardOpenOption.APPEND);
				} catch(IOException ioe) {
					errorMessage = ioe.getMessage();
				}
			} else {
				errorMessage = "Failed to write script file";				
			}
			if(errorMessage != null)
				throw new KnimeScriptingException(errorMessage);
		}
	
		
		if(useScript) {
			// (3) add user script to script file
			try {
				Files.write(scriptFile.toPath(), super.prepareScript().getBytes(), StandardOpenOption.APPEND);
			} catch (IOException ioe) {
				throw new KnimeScriptingException("Failed to write script file: " + ioe.getMessage());
			}

			// (4) add write-calls to script file
			File f = m_tempFiles.get(PY_OUTVAR_BASE_NAME);
			String errorMessage = null;
			if(f != null) {
				try {
					String writeCSVCmd = "\n\nwrite_csv(r\"" + f.getCanonicalPath() + "\"," + PY_OUTVAR_BASE_NAME + ")\n";
					Files.write(scriptFile.toPath(), writeCSVCmd.getBytes(), StandardOpenOption.APPEND);
				} catch(IOException ioe) {
					errorMessage = ioe.getMessage();
				}
			} else {
				errorMessage = "Failed to write script file";				
			}
			if(errorMessage != null)
				throw new KnimeScriptingException(errorMessage);
		}
	}

	/**
	 * retrieve path to python executable from preferences
	 * 
	 * @param preferences
	 * @return	python-exec-path
	 */
	protected String getPythonExecutable(IPreferenceStore preferences) {
		String whichPy = preferences.getString(PythonPreferenceInitializer.PYTHON_USE_2);
		String pythonExecPath = "";
		if(whichPy.equals(PythonPreferenceInitializer.PY2))
			pythonExecPath = preferences.getString(PythonPreferenceInitializer.PYTHON_2_EXECUTABLE);
		if(whichPy.equals(PythonPreferenceInitializer.PY3))
			pythonExecPath = preferences.getString(PythonPreferenceInitializer.PYTHON_3_EXECUTABLE);
		
		return pythonExecPath;
	}
	
	/**
	 * retrieve path to jupyter executable from preferences
	 * 
	 * @param preferences
	 * @return jupyter-exec-path
	 */
	protected String getJupyterExecutable(IPreferenceStore preferences) {
		boolean useJupyter = preferences.getBoolean(PythonPreferenceInitializer.JUPYTER_USE);
		String jupyterExecPath = "";
		if(useJupyter)
			jupyterExecPath = preferences.getString(PythonPreferenceInitializer.JUPYTER_EXECUTABLE);
		
		return jupyterExecPath;
	}
	
	/**
	 * retrieve path to folder for temporary notebooks from preferences
	 * 
	 * @param preferences
	 * @return
	 */
	protected String getJupyterFolder(IPreferenceStore preferences) {	
		return preferences.getString(PythonPreferenceInitializer.JUPYTER_FOLDER);
	}


	/**
	 * fill outport-map with ouput names based on number of outputs <br/>
	 * map-values will be null
	 */
	private void createOutPortMapping() {
		int nOut = this.getNrOutPorts();
		
		m_outPorts = new LinkedHashMap<String, PortObject>();
		
		for(int i = 0; i < nOut; i++) {
			String variableName = PY_OUTVAR_BASE_NAME + (nOut > 1 ? (i + 1) : "");
			m_outPorts.put(variableName, null);
		}	
	}

	/**
	 * deletees all temporary files
	 */
	private void deleteTempFiles() {
		
		for(File tempFile : m_tempFiles.values()) {
			if(tempFile != null)
				try {
					Files.deleteIfExists(tempFile.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * The map stores the Python-names as key and the BDT as value
	 * @param inObjects
	 * 
	 * @throws KnimeScriptingException thrown if more than one generic input ports or unsupported port type
	 */
	public void createInPortMapping(PortObject[] inObjects) throws KnimeScriptingException {

		m_inPorts = new LinkedHashMap<String, PortObject>();

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
					m_inPorts.put(variableName, inport);
					i++;
				}
			}
		}
	}
	
    /**
     * Create necessary temp files
     * 
     * @throws KnimeScriptingException 
     */
    protected void createTempFiles() throws KnimeScriptingException {
        
    	m_tempFiles = new HashMap<String, File>();
    	
    	// prepend a random string to each new file to make it easier to find corresponding temporary files
    	String randomPart = Utils.generateRandomString(6);
    	
    	try {
	    	// Create a new set
	    	for(String label : m_inPorts.keySet()) {
	    		File tempFile = File.createTempFile(randomPart + "_" + label + "_knime2python_", ".csv");
	    		m_tempFiles.put(label, tempFile);
	    	}
	    	for(String label : m_outPorts.keySet()) {
	    		File tempFile = File.createTempFile(randomPart + "_" + label + "_python2knime_", ".csv");
	    		m_tempFiles.put(label, tempFile);
	    	}
	
	    	File scriptFile = File.createTempFile(randomPart + "_analyze_", ".py");
	    	m_tempFiles.put(PY_SCRIPTVAR_BASE_NAME, scriptFile);
	    	
    	} catch (IOException ioe) {
    		throw new KnimeScriptingException("Failed to create temporary files: " + ioe.getMessage());
    	} finally {
    		for(File f : m_tempFiles.values()) {
    			if(f != null)
					try {
						Files.deleteIfExists(f.toPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
    		}
    	}
    }

	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		pushInputToPython(inData, exec);	
		return null;
	}

	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException, CanceledExecutionException {
		pushInputToPython(inData, exec);
		openInPython(exec);
	}
	
	private void openInPython(ExecutionContext exec) throws KnimeScriptingException {
		
		IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();
		boolean useJupyter = preferences.getBoolean(PythonPreferenceInitializer.JUPYTER_USE);
		if(useJupyter) {
			exec.setMessage("Try to open as noteboook (cannot be cancelled");
			openAsNotebook(preferences);
		} else {
			openViaCommandline(preferences);
		}
		
	}

	/**
	 * implementation to 'Open external' via commandline
	 * 
	 * @param preferences
	 * @throws KnimeScriptingException
	 */
	private void openViaCommandline(IPreferenceStore preferences) throws KnimeScriptingException {
		// get executable
		String pythonExecPath = getPythonExecutable(preferences);
		if(pythonExecPath.isEmpty())
			throw new KnimeScriptingException("Path to python executable is not set. Please configure under Preferences > KNIME > Python Scripting.");

		// add shebang to enable execution of py-script
		File scriptFile = m_tempFiles.get(PY_SCRIPTVAR_BASE_NAME);
		try (Writer writer = new BufferedWriter(new FileWriter(scriptFile))) {
			writer.write("#! " + pythonExecPath + " -i\n");
		} catch(IOException ioe) {
			throw new KnimeScriptingException("Failed to prepare script: " + ioe.getMessage());
		} 
		prepareScript(scriptFile, false);
		scriptFile.setExecutable(true);
		
		
		// on Mac open new Terminal and execute python-script
		if (Utils.isMacOSPlatform()) {
			String[] commandLine = new String[]{"open", "-a", "Terminal", scriptFile.getPath()};
			python.executeCommand(commandLine, false);
		}
		// TODO: implement for Windows 
		if (Utils.isWindowsPlatform())
			throw new KnimeScriptingException("Windows not yet supported");
		
		// copy the script in the clipboard
		String actualScript = super.prepareScript();
        if (!actualScript.isEmpty()) {
            StringSelection data = new StringSelection(actualScript);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(data, data);
        }
	}

	/**
	 * implementation 'Open external' as jupyter notebook <br/>
	 * load template notebook and replace placeholders with pathes and script
	 * 
	 * @param preferences
	 * @throws KnimeScriptingException
	 */
	private void openAsNotebook(IPreferenceStore preferences) throws KnimeScriptingException {
		
		String jupyterLocation = getJupyterExecutable(preferences);
		
		if(jupyterLocation.isEmpty())
			throw new KnimeScriptingException("Path to jupyter executable is not set. Please configure under Preferences > KNIME > Python Scripting > Open As Notebook.");
		
		String version = isJupyterInstalled(jupyterLocation);
		
		// get jupyter path
		String jupyterDirString = getJupyterFolder(preferences);
		if(jupyterDirString.isEmpty())
			throw new KnimeScriptingException("Path to jupyter folder is not set. Please configure under Preferences > KNIME > Python Scripting > Open As Notebook.");
		
		// 1) load notebook template and copy as tempfile for modification
		Path nbFile = null;
		try (InputStream utilsStream = getClass().getClassLoader().getResourceAsStream("/de/mpicbg/knime/scripting/python/scripts/template_notebook.ipynb")) {
			nbFile = Files.createTempFile(Paths.get(jupyterDirString), "notebookformat_", ".ipynb");
			Files.copy(utilsStream, nbFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ioe) {
			throw new KnimeScriptingException("Failed to create notebook format file: " + ioe.getMessage());
		}
		
		// 2) replace the following parts with the appropriate file names and the current script
		final String INPUT_PLACEHOLDER = "\"kIn = read_csv(r\\\"/path/to/input.csv\\\")\"";
		final String OUTPUT_PLACEHOLDER = " \"write_csv(pyOut, r\\\"/path/to/output.csv\\\")\"";
		final String SCRIPT_PLACEHOLDER = "\"source\": [\n" + 
				"    \"pyOut = kIn\"\n" + 
				"   ]";
		
		/*
		 * supposed to look like this
		 * "kIn = read_csv(r\"***filename***\")",
		 * "kIn2 = read_csv(r\"***anotherfilename***\")"
		 */
		
		List<String> loadInputList = new LinkedList<String>();
		for(String inLabel : m_inPorts.keySet()) {
			File f = m_tempFiles.get(inLabel);
			if(f != null) {	
				String current = "\"" + inLabel + " = read_csv(r\\\"" + f.getPath() +"\\\")\\n\"";
				loadInputList.add(current);
			} else {
				throw new KnimeScriptingException("Failed to write script file");				
			}
		}
		String loadInput = String.join(",\n", loadInputList);
		
		/*
		 * supposed to look like this
		 * "write_csv(r\"***filename***\",pyOut)",
		 * "write_csv(r\"***otherfilename***\",pyOut2)"
		 */
		
		List<String> saveOutputList = new LinkedList<String>();
		for(String outLabel : m_outPorts.keySet()) {
			File f = m_tempFiles.get(outLabel);
			if(f != null) {	
				String current = "\"write_csv(r\\\"" + f.getPath() +"\\\"," + outLabel+ ")\\n\"";
				saveOutputList.add(current);
			} else {
				throw new KnimeScriptingException("Failed to write script file");				
			}
		}
		String saveOutput = String.join(",\n", saveOutputList);
		
		// prepare script
		/*
		 * supposed to look like this
		 * 	"pyOut = kIn\n",
		 * 	"for i in [1,2,3,10]:\n",
		 * 	"    print(i)"
		 */
		
		final String tabReplace = "    ";
		String wholeScript = super.prepareScript();
		String[] splittedByNewline = wholeScript.split("\\R", -1);
		String script = "\"source\": [\n";
		List<String> lineList = new LinkedList<String>();
		for(String line : splittedByNewline) {
			line = line.replace("\t", tabReplace);
			String current = tabReplace + "\"" + line + "\\n\"";
			lineList.add(current);
		}		
		script = script + String.join(",\n", lineList) + "\n   ]";
		
		Charset charset = StandardCharsets.UTF_8;
		String content;
		try {
			content = new String(Files.readAllBytes(nbFile), charset);
			content = content.replace(INPUT_PLACEHOLDER, loadInput);
			content = content.replace(OUTPUT_PLACEHOLDER, saveOutput);
			content = content.replace(SCRIPT_PLACEHOLDER, script);			
			Files.write(nbFile, content.getBytes(charset));
		} catch (IOException ioe) {
			throw new KnimeScriptingException("Failed to modify notebook format file: " + ioe.getMessage());
		}
		
		// 3) 
		launchNotebook(jupyterLocation, nbFile);
	}

	/**
	 * launch jupyter notebook with notebookfile
	 * 
	 * @param jupyterLocation
	 * @param nbFile
	 * @throws KnimeScriptingException
	 */
	private void launchNotebook(String jupyterLocation, Path nbFile) throws KnimeScriptingException {
		
		assert python != null;
		
		try {
			python.executeCommand(new String[]{jupyterLocation, "notebook", nbFile.toString()}, false);
		} catch (Exception re) {
			throw new KnimeScriptingException("Failed while launching Jupyter: " + re.getMessage());
		}

	}

	/**
	 * check whether jupyter version can be obtained
	 * 
	 * @param jupyterLocation
	 * @return
	 * @throws KnimeScriptingException
	 */
	private String isJupyterInstalled(String jupyterLocation) throws KnimeScriptingException {
		
		assert python != null;
		String outString = ""; 
		
		CommandOutput output;
		try {
			output = python.executeCommand(new String[]{jupyterLocation, "notebook", "--version"});
		} catch (Exception re) {
			throw new KnimeScriptingException("Failed while checking for Jupyter Version: " + re.getMessage());
		}
	
		if(output.hasStandardOutput()) {			
			outString= String.join("\n", output.getStandardOutput());
		}
		if(output.hasErrorOutput())
			throw new KnimeScriptingException("Jupyter check failed: " + String.join("\n", output.getErrorOutput()));
		
		return outString;
	}

	/**
	 * create DataCell based on value in string format and a given datatype
	 * 
	 * @param value		value as string representation
	 * @param dType		required data type
	 * 
	 * @return			{@link DataCell}
	 * @throws KnimeScriptingException
	 */
	private DataCell createCell(String value, DataType dType) throws KnimeScriptingException {
		
		if(dType.equals(StringCellFactory.TYPE)) {
			return new StringCell(value);
		}
		if(dType.equals(DoubleCellFactory.TYPE)) {
			try {
				double d = Double.parseDouble(value);
				return new DoubleCell(d);
			} catch (NumberFormatException nfe) {
				throw new KnimeScriptingException(nfe.getMessage());
			}
		}
		if(dType.equals(IntCellFactory.TYPE)) {
			try {
				int i = Integer.parseInt(value);
				return new IntCell(i);
			} catch (NumberFormatException nfe) {
				throw new KnimeScriptingException(nfe.getMessage());
			}
		}
		if(dType.equals(BooleanCellFactory.TYPE)) {
			boolean b = Boolean.parseBoolean(value);
			return BooleanCellFactory.create(b);
		}
		if(dType.equals(LocalDateTimeCellFactory.TYPE)) {		
			return LocalDateTimeCellFactory.create(value, PY_dateFormatter);
		}
		
		return null;
	}

	/**
	 * map Python-type definition to KNIME type definition
	 * 
	 * @param cType
	 * @return	{@link DataType}
	 */
	private DataType getKnimeDataType(String cType) {
		
		// need to get data type from the factory as there
		// is LocalDateTimeCell has no public type available
		
		switch(cType) {
		case PY_TYPE_OBJECT: return StringCellFactory.TYPE;
		case PY_TYPE_BOOL: return BooleanCellFactory.TYPE;
		case PY_TYPE_FLOAT: return DoubleCellFactory.TYPE;
		case PY_TYPE_INT: return IntCellFactory.TYPE;
		case PY_TYPE_DATETIME: return LocalDateTimeCellFactory.TYPE;
		default: return null;
		}
	}

	/**
	 * @return KNIME table spec
	 */
	private DataTableSpec createDataTableSpec(Map<String, DataType> columns) {
		
		List<DataColumnSpec> cSpecList = new LinkedList<DataColumnSpec>();

		for(String col : columns.keySet()) {
			DataColumnSpecCreator newColumn = new DataColumnSpecCreator(col, columns.get(col));
			cSpecList.add(newColumn.createSpec());
		}
		
		DataColumnSpec[] cSpecArray = new DataColumnSpec[cSpecList.size()];
		cSpecArray = cSpecList.toArray(cSpecArray);
		
		DataTableSpec tSpec = new DataTableSpec("Result from R", cSpecArray);
		
		return tSpec;
	}

}



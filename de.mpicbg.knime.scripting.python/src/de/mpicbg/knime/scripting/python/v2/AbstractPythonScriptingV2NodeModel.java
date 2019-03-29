package de.mpicbg.knime.scripting.python.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import com.opencsv.CSVReader;
import com.opencsv.CSVParserBuilder;
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


public abstract class AbstractPythonScriptingV2NodeModel extends AbstractScriptingNodeModel {
	
	public static final String PY_INVAR_BASE_NAME = "kIn";
	public static final String PY_OUTVAR_BASE_NAME = "pyOut";
	public static final String PY_SCRIPTVAR_BASE_NAME = "pyScript";
	
	public static final String CFG_SCRIPT_DFT = "pyOut = kIn";
	
	Map<String, File> m_tempFiles;
	Map<String, PortObject> m_inPorts;
	Map<String, PortObject> m_outPorts;

    protected Python python;

    protected IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    //'object','bool','float','int','datetime64[ns]'
    public static final String PY_TYPE_OBJECT = "object";
    public static final String PY_TYPE_BOOL= "bool";
    public static final String PY_TYPE_FLOAT = "float64";
    public static final String PY_TYPE_INT = "int64";
    public static final String PY_TYPE_DATETIME = "datetime64[ns]";
    public static final String PY_TYPE_INDEX = "INDEX";
    
    public static final DateTimeFormatter PY_dateFormatter = initDateTime();
    
    private List<String> m_stdOut = null;
    
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
		ExecutionMonitor subExec = exec.createSubProgress(1.0 / 2);
		
		// assign ports to Python variable names
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
	private void pushTableToPython(BufferedDataTable inTable, String varName, File tempFile, ExecutionMonitor exec) 
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
	 * main method to retrieve data from R to provide the output ports
	 * NOTE: this method closes the connection in case of Exceptions
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


	private PortObject pullTableFromPython(String out, ExecutionContext exec, double subProgress) throws CanceledExecutionException, IOException, KnimeScriptingException {
		
		ExecutionMonitor execM = exec.createSubProgress(subProgress);
		execM.setMessage("retrieve " + out);
		
		File tempFile = m_tempFiles.get(out);
		
		assert tempFile != null;
		
		// read number of lines (approximate, as line breaks may occur within quotes
		int nLines = -1;
	
		try (FileInputStream fis = new FileInputStream(tempFile)) {
			nLines = FileUtils.countLines(fis);
		}
	
		
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


	protected void runScript(ExecutionMonitor exec) throws KnimeScriptingException {
			
		try {
			runScriptImpl(exec);
		} catch (KnimeScriptingException kse) {
			deleteTempFiles();
			throw kse;
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
		Writer writer = null;
		File scriptFile = m_tempFiles.get(PY_SCRIPTVAR_BASE_NAME);
		try {
			writer = new BufferedWriter(new FileWriter(scriptFile));
			prepareScript(scriptFile, true);
		} catch(IOException ioe) {
			throw new KnimeScriptingException("Failed to prepare script: " + ioe.getMessage());
		} finally {
			if(writer != null)
				try {
					writer.close();
				} catch (IOException ioe) {
					throw new KnimeScriptingException("Failed to close script file: " + ioe.getMessage() + "\n" + scriptFile.getAbsolutePath());
				}
		}
	
	
		// run script
	
		boolean local = preferences.getBoolean(PythonPreferenceInitializer.PYTHON_LOCAL);
		if(!local) {
			this.setWarningMessage("Remote python processing is not supported anymore. The local python will be used.");
		}
		String pythonExecPath = preferences.getString(PythonPreferenceInitializer.PYTHON_EXECUTABLE);
	
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
			throw new KnimeScriptingException("Error in processing: " + output.getErrorOutput());
	
	}


	private void createOutPortMapping() {
		int nOut = this.getNrOutPorts();
		
		m_outPorts = new LinkedHashMap<String, PortObject>();
		
		for(int i = 0; i < nOut; i++) {
			String variableName = PY_OUTVAR_BASE_NAME + (nOut > 1 ? (i + 1) : "");
			m_outPorts.put(variableName, null);
		}	
	}

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
	 * The map stores the R-names as key and the BDT as value; in case of RPorts, the name is 'generic'
	 * @param inObjects
	 * @return
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
     * @param inPorts 
     * @return 
     * @throws KnimeScriptingException 
     */
    protected void createTempFiles() throws KnimeScriptingException {
        
    	m_tempFiles = new HashMap<String, File>();
    	
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
		// TODO Auto-generated method stub
		
	}
	
	protected void prepareScript(File scriptFile,boolean useScript) throws KnimeScriptingException {
		// CSV read/write functions
		InputStream utilsStream = getClass().getClassLoader().getResourceAsStream("/de/mpicbg/knime/scripting/python/scripts/PythonCSVUtils2.py");

		try {
			Files.copy(utilsStream, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ioe) {
			throw new KnimeScriptingException("Failed to write script file: " + ioe.getMessage());
		} finally {
			try {
				utilsStream.close();
			} catch (IOException ioe) {
				throw new KnimeScriptingException("Failed to close source script file: " + ioe.getMessage());
			}
		}

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

		if(useScript)
			try {
				Files.write(scriptFile.toPath(), super.prepareScript().getBytes(), StandardOpenOption.APPEND);
			} catch (IOException ioe) {
				throw new KnimeScriptingException("Failed to write script file: " + ioe.getMessage());
			}

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



    /**
     * Delete all temp files if they exist and the node is so configured
     */
 /*   


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


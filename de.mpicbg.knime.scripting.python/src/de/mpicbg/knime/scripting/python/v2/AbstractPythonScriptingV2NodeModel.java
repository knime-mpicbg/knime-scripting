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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.period.PeriodCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import de.mpicbg.knime.knutils.FileUtils;
import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptingCoreBundleActivator;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.core.prefs.JupyterKernelSpec;
import de.mpicbg.knime.scripting.core.prefs.JupyterKernelSpecsEditor;
import de.mpicbg.knime.scripting.core.prefs.ScriptingPreferenceInitializer;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;
import de.mpicbg.knime.scripting.python.srv.CommandOutput;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.Python;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel.PythonInputMode.Flag;

/**
 * abstract model class for all python scripting nodes (version 2)
 * take care of data transfer methods from/to python
 * 
 * @author Antje Janosch
 *
 */
public abstract class AbstractPythonScriptingV2NodeModel extends AbstractScriptingNodeModel {
	
	/** true, if running on a Windows OS */
	protected final boolean isWindowsPlatform= Utils.isWindowsPlatform();
	
	/**
	 * constants
	 */	
	protected static final String PY_INVAR_BASE_NAME = "kIn";
	protected static final String PY_OUTVAR_BASE_NAME = "pyOut";
	protected static final String PY_SCRIPTVAR_BASE_NAME = "pyScript";
	
	/**
	 * temp files and input/output ports
	 */
	
	/** String with random characters to create temp files with a common part */
	private String m_randomPart;
	
	/** map of temporary files and their assigned label as key */
	private Map<String, File> m_tempFiles = new HashMap<String, File>();
	
	private Map<String, PortObject> m_inPorts;
	private Map<String, PortObject> m_outPorts;		// knime tables only
	
	private List<String> m_inKeys = new ArrayList<>();
	
	// store sdtout messages from Python script execution
    private List<String> m_stdOut = new LinkedList<String>();

	// python executor
    protected Python python;

    // python preferences
    protected IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    // supported pandas column types
    // 'object','bool','float','int','datetime64[ns]'
    public static final String PY_TYPE_OBJECT = "object";
    public static final String PY_TYPE_BOOL= "bool";
    public static final String PY_TYPE_FLOAT = "float64";
    public static final String PY_TYPE_LONG = "int64";
    public static final String PY_TYPE_DATETIME = "datetime64[ns]";
    public static final String PY_TYPE_TIMEDELTA = "timedelta64[ns]";
    public static final String PY_TYPE_INDEX = "INDEX";
    
    // date-time format send to Python
    private final DateTimeFormatter TO_PY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter TO_PY_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnn");
    private final DateTimeFormatter TO_PY_TIME = DateTimeFormatter.ofPattern("HH:mm:ss.nnnnnnnnn");
    
    // date-time format exported by Python %Y-%m-%dT%H:%M:%S.%f (microsecond precision, not yet possible to return nanosecond precision)
    private final DateTimeFormatter PY_dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    
    

    /**
     * WRITE:	serialize python input to file
     * READ:	read serialized input from file
     * 
     * @author Antje Janosch
     *
     */
    public static class PythonInputMode {
    	
    	Flag flag;
    	File file;
    	
        public enum Flag
        {
          READ, WRITE, IGNORE
        }
    	
        public PythonInputMode(File file, Flag flag) {
        	this.flag = flag;
        	this.file = file;
        }
        
        Flag getFlag() {
        	return this.flag;
        }
        
        File getFile() {
        	return this.file;
        }
        
        public static PythonInputMode ignoreFlag() {
        	return new PythonInputMode(null, Flag.IGNORE);
        }
    } 
    
    
    
    /**
     * constructor
     * 
	 * @param inPorts
	 * @param outPorts
	 * @param columnSupport
	 */
	public AbstractPythonScriptingV2NodeModel(PortType[] inPorts, PortType[] outPorts, PythonColumnSupport columnSupport, String defaultScript) {
		super(inPorts, outPorts, columnSupport);
	}

	/**
	 * constructor
	 * 
	 * @param inPorts
	 * @param outPorts
	 */
	public AbstractPythonScriptingV2NodeModel(PortType[] inPorts, PortType[] outPorts) {
		super(inPorts, outPorts, new PythonColumnSupport());
	}

	/**
	 * constructor with node configuration object
	 * 
	 * @param nodeModelConfig
	 */
	public AbstractPythonScriptingV2NodeModel(ScriptingModelConfig nodeModelConfig) {
		super(nodeModelConfig);
	}

	
	/**
	 * main method to push available inputs to Python
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
				removeTempFiles();
				throw kse;
			}
		}
		
		m_inKeys.clear();;
		m_inKeys.addAll(m_inPorts.keySet());
	}
	
	/**
	 * write one KNIME table to temporary CSV file
	 * 
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
	    	
	    	Class<? extends DataCell> cellClass = dType.getCellClass();
	    	
	    	//numeric classes
	    	if(cellClass.equals(BooleanCell.class)) {
	    		supportedColumns.put(cName, PY_TYPE_BOOL);   
	    		columnsIndicees.put(cName, i);
	    	}
	    	if(cellClass.equals(IntCell.class)) {
	    		supportedColumns.put(cName, PY_TYPE_LONG);  
	    		columnsIndicees.put(cName, i);
	    	}
	    	if(cellClass.equals(DoubleCell.class)) {
	    		supportedColumns.put(cName, PY_TYPE_FLOAT); 
	    		columnsIndicees.put(cName, i);
	    	}
	    	
	    	// date/time classes
	    	if(cellClass.equals(LocalDateTimeCell.class) ||
	    			cellClass.equals(LocalDateCell.class) ||
	    			cellClass.equals(LocalTimeCell.class))	    	
	    	{
	    		supportedColumns.put(cName, PY_TYPE_DATETIME);  
	    		columnsIndicees.put(cName, i);
	    		continue;
	    	}
	    	
	    	// period / duration classes
	    	if(cellClass.equals(DurationCell.class) || cellClass.equals(PeriodCell.class)) {
	    		supportedColumns.put(cName, PY_TYPE_TIMEDELTA);
	    		columnsIndicees.put(cName, i);
	    		continue;
	    	}
	    	
	    	// string compatible classes
	    	if(dType.isCompatible(StringValue.class)) {
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
	            //CSVWriter.DEFAULT_ESCAPE_CHARACTER,
	            '\\',
	            CSVWriter.DEFAULT_LINE_END);
	    
	    String[] columnNames = supportedColumns.keySet().toArray(new String[supportedColumns.size()]);
	    String[] columnTypes = supportedColumns.values().toArray(new String[supportedColumns.size()]);
	    
	    // write column names (at least 'Row ID')
	    csvWriter.writeNext(columnNames, false);
	    csvWriter.writeNext(columnTypes, false);
	    
	    long nRows = inTable.size();
	    
	    /*
	     * write KNIME table content row by row, column by column to CSV
	     */
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
	    					long val = ((LongValue) cell).getLongValue();
	    					columnValues.add(Long.toString(val));
	    				}
	    				if(dType.getCellClass().equals(DoubleCell.class)) {
	    					double val = ((DoubleValue) cell).getDoubleValue();
	    					columnValues.add(Double.toString(val));
	    				}
	    				
	    				Class<? extends DataCell> cellClass = dType.getCellClass();
    					
    					if(cellClass.equals(LocalDateCell.class)) {
    						LocalDate date = ((LocalDateCell) cell).getLocalDate();
    						columnValues.add(date.format(TO_PY_DATE));
    						continue;
    					}
    					if(cellClass.equals(LocalTimeCell.class)) {
    						LocalTime time = ((LocalTimeCell) cell).getLocalTime();
    						columnValues.add(time.format(TO_PY_TIME));
    						continue;
    					}
    					if(cellClass.equals(LocalDateTimeCell.class)) {
    						LocalDateTime datetime = ((LocalDateTimeCell) cell).getLocalDateTime();
    						columnValues.add(datetime.format(TO_PY_DATETIME));
    						continue;
    					}
	    				if(cellClass.equals(DurationCell.class)) {
	    					Duration duration = ((DurationCell) cell).getDuration();	
	    					columnValues.add(duration.toString());
	    					continue;
	    				}
	    				if(cellClass.equals(PeriodCell.class)) {
	    					Period period = ((PeriodCell) cell).getPeriod();
	    					columnValues.add(period.toString());
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
		
		int nOut = getNrOutPorts();
		
		String[] outTableLabels = m_outPorts.keySet().toArray(new String[m_outPorts.size()]);
		
		int outTableCounter = 0;
		
		PortObject[] ports = new PortObject[nOut];
		
		// for each output port
		for(int i = 0; i < getNrOutPorts(); i++) {
			PortType pType = this.getOutPortType(i);
			
			if(pType.equals(BufferedDataTable.TYPE)) {
				transferToExec.setMessage("Pull table");
				String label = outTableLabels[outTableCounter];
				double subProgress = 1.0/nOut;
				PortObject outTable = null;
				try {
					outTable = pullTableFromPython(label, exec, subProgress);
				} catch(IOException ioe) {
					removeTempFiles();
					throw new KnimeScriptingException("Failed to read table: " + ioe.getMessage());
				} 
				ports[i] = outTable;
				outTableCounter++;
			}
		}
		
		return ports;
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
	 * returns the file of the Python script
	 * 
	 * @return {@see File}
	 */
	protected File getScriptFile() {
		return m_tempFiles.get(PY_SCRIPTVAR_BASE_NAME);
	}

	/**
	 * execute python script and make sure that temp files are deleted if something goes wrong
	 * 
	 * @param exec
	 * @throws KnimeScriptingException
	 */
	protected void runScript(ExecutionMonitor exec) throws KnimeScriptingException {
		
		// empty output from previous executions
		m_stdOut.clear();
			
		try {
			exec.setMessage("Evaluate Python-script (cannot be cancelled)");
			runScriptImpl(m_tempFiles.get(PY_SCRIPTVAR_BASE_NAME));
		} catch (KnimeScriptingException kse) {
			removeTempFiles();
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
	 * @param scriptFile 
	 * @throws KnimeScriptingException
	 */
	protected void runScriptImpl(File scriptFile) throws KnimeScriptingException {

		IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

		// run script
	
		// get executable
		String pythonExecPath = getPythonExecutable(preferences);
		if(pythonExecPath.isEmpty())
			throw new KnimeScriptingException("Path to python executable is not set. Please configure under Preferences > KNIME > Python Scripting.");
	
		// necessary to have an instance for recreating the view image
		if(python == null)
			python = new LocalPythonClient();
		
		CommandOutput output;
		String[] command;		
		
		try {
			if(isWindowsPlatform)
				command = new String[] {"powershell.exe", "-Command", pythonExecPath, scriptFile.getCanonicalPath()};
			else 
				command = new String[] {pythonExecPath, scriptFile.getCanonicalPath()};
			output = python.executeCommand(command);
			
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
	 * {@inheritDoc}
	 */
	@Override
	protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		super.configure(inSpecs);
		
		// check input data tables for unsupported column types to throw a warning
		List<String> droppedColumns;
		List<String> warnings = new LinkedList<>();
		
		int inSpecIdx = 0;
		for (PortObjectSpec inSpec : inSpecs) {
			if (inSpec != null) 
				if(inSpec instanceof DataTableSpec) {
					DataTableSpec tableSpec = (DataTableSpec) inSpec;
					droppedColumns = new LinkedList<String>();
					
					for(int i = 0; i < tableSpec.getNumColumns(); i++) {
						boolean transfer = false;
						DataColumnSpec cSpec = tableSpec.getColumnSpec(i);
						DataType dType = cSpec.getType();
						String cName = cSpec.getName();
						
						Class<? extends DataCell> cellClass = dType.getCellClass();
				    	
				    	if(cellClass.equals(BooleanCell.class)) 
				    		transfer = true;

				    	if(cellClass.equals(IntCell.class)) 
				    		transfer = true;

				    	if(cellClass.equals(DoubleCell.class)) 
				    		transfer = true;

				    	if(cellClass.equals(LocalTimeCell.class) ||
				    		cellClass.equals(LocalDateCell.class) ||
				    		cellClass.equals(LocalDateTimeCell.class)) 
				    		transfer = true;
				   
				    	if(cellClass.equals(DurationCell.class) ||
				    			cellClass.equals(PeriodCell.class))
				    			transfer = true;
				    	if(dType.isCompatible(StringValue.class)) 
				    		transfer = true;
				    	
				    	if(!transfer) 
				    		droppedColumns.add(cName);
				 
					}
					
				    if(!droppedColumns.isEmpty()) {
			    	warnings.add("Unsupported data types at input " + inSpecIdx + ". The following columns will not be transfered: " + 
				    		 String.join(",",droppedColumns));
				    }
				}
			inSpecIdx ++;
		}
		
		if(!warnings.isEmpty())
			this.setWarningMessage(String.join("\n", warnings));
		
		return null; 
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
	protected void prepareScript(BufferedWriter scriptFileWriter, boolean useScript, PythonInputMode flag) throws KnimeScriptingException {
		
		String kseMessage = "Failed to write script file: ";
		// CSV read/write functions
		
		// (1) write template script to script file
		try (InputStream utilsStream = getClass().getClassLoader().getResourceAsStream("/de/mpicbg/knime/scripting/python/scripts/PythonCSVUtils2.py")) {
			String pyScriptString = FileUtils.readFromRessource(utilsStream);
			scriptFileWriter.write(pyScriptString);
		}catch(IOException ioe) {
			throw new KnimeScriptingException(kseMessage, ioe.getMessage());
		}
		
		Flag fl = flag.getFlag();
		
		// (2a) decide where to read the data from
		if(!fl.equals(Flag.READ)) {	// read data from CSV and if required write to pickle
	
			boolean write = fl.equals(Flag.WRITE);
			if(write) {		// open pickle to write data that has been read from CSV
				
				File pickleFile = flag.getFile();
				assert pickleFile != null;
				
				// remove .db extension as this is added automatically by the python pickle.open call 
				// to the given name
				String pickleFilePath = pickleFile.getAbsolutePath();
				
				try {
					scriptFileWriter.newLine();
					scriptFileWriter.newLine();
					
					if(isWindowsPlatform)
						pickleFilePath  = pickleFilePath.replace('\\', '/');
					
					String toScriptFile = "with open('" + pickleFilePath + "', 'wb') as pfile:\n";
					scriptFileWriter.write(toScriptFile);
				} catch(IOException ioe) {
					throw new KnimeScriptingException(kseMessage, ioe.getMessage());
				}	
			}
				
			// (2) add read-calls to script file
			for(String inLabel : m_inPorts.keySet()) {
				File f = m_tempFiles.get(inLabel);
				String errorMessage = null;
				if(f != null) {
					try {	
						scriptFileWriter.newLine();
						scriptFileWriter.newLine();
						
						String filename = f.getCanonicalPath();
						if(isWindowsPlatform)
							filename  = filename.replace('\\', '/');
						
						if(write) scriptFileWriter.write("\t");
						String readCSVCmd = inLabel + " = read_csv(r\"" + filename + "\")";
						scriptFileWriter.write(readCSVCmd);
						scriptFileWriter.newLine();
						
						if(write) {
							readCSVCmd = "\tpickle.dump(" + inLabel + ", pfile)";
							scriptFileWriter.write(readCSVCmd);
							scriptFileWriter.newLine();
						}
					} catch(IOException ioe) {
						errorMessage = ioe.getMessage();
					}
				} else {
					errorMessage = "tempory file error (NPE)";				
				}
				if(errorMessage != null)
					throw new KnimeScriptingException(kseMessage, errorMessage);
			}
	
		}
		if(fl.equals(Flag.READ)) {	// read data from pickle file
			
			File pickleFile = flag.getFile();
			assert pickleFile != null;
			
			// remove .db extension as this is added automatically by the python pickle.open call 
			// to the given name
			String pickleFilePath = pickleFile.getAbsolutePath();
			
			try {
				scriptFileWriter.newLine();
				scriptFileWriter.newLine();
				
				if(isWindowsPlatform)
					pickleFilePath  = pickleFilePath.replace('\\', '/');
				
				String toScriptFile = "with open('" + pickleFilePath + "', 'rb') as pfile:";
				scriptFileWriter.write(toScriptFile);
			} catch(IOException ioe) {
				throw new KnimeScriptingException(kseMessage, ioe.getMessage());
			}	
			
			for(String inKey : m_inKeys) {
				try {
					scriptFileWriter.newLine();
					//scriptFileWriter.newLine();
					
					//String readCSVCmd = inKey + " = s['" + inKey + "']";
					String readCSVCmd = "\t" + inKey + "= pickle.load(pfile)";
					scriptFileWriter.write(readCSVCmd);
					scriptFileWriter.newLine();
					
				} catch (IOException ioe) {
					throw new KnimeScriptingException(kseMessage, ioe.getMessage());
				}
		
			}
				
		}
		
		if(useScript) {
			// (3) add user script to script file
			try {
				scriptFileWriter.write(super.prepareScript());
			} catch (IOException ioe) {
				throw new KnimeScriptingException(kseMessage, ioe.getMessage());
			}

			// (4) add write calls to script file
			if(!fl.equals(Flag.READ)) {
				
				for(String outLabel : m_outPorts.keySet()) {
					File f = m_tempFiles.get(outLabel);
					String errorMessage = null;
					if(f != null) {
						try {
							scriptFileWriter.newLine();
							scriptFileWriter.newLine();
							
							String filename = f.getCanonicalPath();
							if(isWindowsPlatform)
								filename  = filename.replace('\\', '/');

							String writeCSVCmd = "write_csv(r\"" + filename + "\"," + outLabel + ")";
							scriptFileWriter.write(writeCSVCmd);
							scriptFileWriter.newLine();

						} catch(IOException ioe) {
							errorMessage = ioe.getMessage();
						}
					} else {
						errorMessage = "tempory file error (NPE)";				
					}
					if(errorMessage != null)
						throw new KnimeScriptingException(kseMessage, errorMessage);
				}
			}
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
		return preferences.getString(ScriptingPreferenceInitializer.JUPYTER_EXECUTABLE);
	}
	
	/**
	 * retrieve jupyter mode from preferences
	 * 
	 * @param preferences
	 * @return
	 */
	protected String getJupyterMode(IPreferenceStore preferences) {	
		return preferences.getString(ScriptingPreferenceInitializer.JUPYTER_MODE);
	}
	
	/**
	 * retrieve path to folder for temporary notebooks from preferences
	 * 
	 * @param preferences
	 * @return
	 */
	protected String getJupyterFolder(IPreferenceStore preferences) {	
		return preferences.getString(ScriptingPreferenceInitializer.JUPYTER_FOLDER);
	}


	/**
	 * fill outport-map with output names based on number of table outputs <br/>
	 * map-values will be null
	 */
	private void createOutPortMapping() {
		int nOut = this.getNrOutPorts();
		
		m_outPorts = new LinkedHashMap<String, PortObject>();
		
		for(int i = 0; i < nOut; i++) {
			PortType type = this.getOutPortType(i);
			if(type.equals(BufferedDataTable.TYPE)) {
				String variableName = PY_OUTVAR_BASE_NAME + (nOut > 1 ? (i + 1) : "");
				m_outPorts.put(variableName, null);
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
        
    	// make sure existing files are deleted (from 'Open External e.g.)
    	removeTempFiles();
    	m_tempFiles = new HashMap<String, File>();  
    	
    	try {
	    	// Create a new set
	    	for(String label : m_inPorts.keySet()) {
	    		File tempFile = File.createTempFile(m_randomPart + "_" + label + "_knime2python_", ".csv");
	    		m_tempFiles.put(label, tempFile);
	    	}
	    	for(String label : m_outPorts.keySet()) {
	    		File tempFile = File.createTempFile(m_randomPart + "_" + label + "_python2knime_", ".csv");
	    		m_tempFiles.put(label, tempFile);
	    	}
	
	    	File scriptFile = File.createTempFile(m_randomPart + "_analyze_", ".py");
	    	m_tempFiles.put(PY_SCRIPTVAR_BASE_NAME, scriptFile);
	    	
    	} catch (IOException ioe) {
    		removeTempFiles();
    		throw new KnimeScriptingException("Failed to create temporary files: " + ioe.getMessage());
    	} 
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	protected void onDispose() {
		super.onDispose();
		removeTempFiles();
	}

    /**
     * adds a temp file to the temp file map
     * 
     * @param label
     * @param file
     * @return
     */
	protected boolean addTempFile(String label, File file) {
    	return m_tempFiles.put(label, file) != null;
    }
    
	/**
	 * retrieve temp file based on its label
	 * 
	 * @param label
	 * @return {@link File}
	 */
    protected File getTempFile(final String label) {
    	return m_tempFiles.get(label);
    }
    
    /**
     * retrieve current random string
     * 
     * @return {@link String}
     */
    protected String getRandomPart() {
		return m_randomPart;
	}
    
    /**
     * deletes all temporary files stored in temp file map
     */
    protected void removeTempFiles() {
    	for(File f : m_tempFiles.values()) {
			if(f != null)
				try {
					Files.deleteIfExists(f.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		m_randomPart = Utils.generateRandomString(6);
		pushInputToPython(inData, exec);	
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException, CanceledExecutionException {
		m_randomPart = Utils.generateRandomString(6);
		pushInputToPython(inData, exec);
		openInPython(exec);
	}
	
	/**
	 * implementation of the 'Open external' mode
	 * 
	 * @param exec
	 * @throws KnimeScriptingException
	 */
	private void openInPython(ExecutionContext exec) throws KnimeScriptingException {
		
		IPreferenceStore pythonPreferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();
		boolean useJupyter = pythonPreferences.getBoolean(PythonPreferenceInitializer.JUPYTER_USE);
		if(useJupyter) {
			exec.setMessage("Try to open as noteboook (cannot be cancelled");
			openAsNotebook(pythonPreferences);
		} else {
			openViaCommandline(pythonPreferences);
		}
		
	}

	/**
	 * implementation of 'Open external' via commandline
	 * 
	 * @param preferences
	 * @throws KnimeScriptingException
	 */
	private void openViaCommandline(IPreferenceStore preferences) throws KnimeScriptingException {
		// get executable
		String pythonExecPath = getPythonExecutable(preferences);
		if(pythonExecPath.isEmpty())
			throw new KnimeScriptingException("Path to python executable is not set. Please configure under Preferences > KNIME > Python Scripting.");

		
		File scriptFile = m_tempFiles.get(PY_SCRIPTVAR_BASE_NAME);
			
		try(BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(scriptFile, true))) {
			// add shebang to enable execution of py-script
			scriptWriter.write("#! " + pythonExecPath + " -i\n");
			
			prepareScript(scriptWriter, false, PythonInputMode.ignoreFlag());
		} catch (IOException ioe) {
			removeTempFiles();
			throw new KnimeScriptingException("Failed to prepare script: " + ioe.getMessage());
		}
		
		scriptFile.setExecutable(true);
		
		// on Mac open new Terminal and execute python-script
		if (Utils.isMacOSPlatform()) {
			String[] commandLine = new String[]{"open", "-a", "Terminal", scriptFile.getPath()};
			python.executeCommand(commandLine, false);
		}
		// TODO: implement for  
		if (isWindowsPlatform) {
			String[] commandLine = new String[]{"powershell.exe", "-Command", pythonExecPath, scriptFile.getPath()};
			python.executeCommand(commandLine, false);
			//throw new KnimeScriptingException(" not yet supported");
		}
		
		// copy the script in the clipboard
		String actualScript = super.prepareScript();
        if (!actualScript.isEmpty()) {
            StringSelection data = new StringSelection(actualScript);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(data, data);
        }
	}

	/**
	 * implementation of 'Open external' as jupyter notebook <br/>
	 * load template notebook and replace placeholders with pathes and script
	 * 
	 * @param pythonPreferences
	 * @throws KnimeScriptingException
	 */
	private void openAsNotebook(IPreferenceStore pythonPreferences) throws KnimeScriptingException {
		
		IPreferenceStore jupyterPreferences = ScriptingCoreBundleActivator.getDefault().getPreferenceStore();
		
		String jupyterLocation = getJupyterExecutable(jupyterPreferences);
		String jupyterMode = getJupyterMode(jupyterPreferences);
		
		String whichPy = pythonPreferences.getString(PythonPreferenceInitializer.PYTHON_USE_2);
		JupyterKernelSpec jupyterKernel = getJupyterKernel(jupyterPreferences, whichPy);
		
		
		if(jupyterLocation.isEmpty())
			throw new KnimeScriptingException("Path to jupyter executable is not set. Please configure under Preferences > KNIME > Community Scripting > JupyterSettings.");
		if(jupyterKernel == null)
			throw new KnimeScriptingException("No kernelspec available. Please configure under Preferences > KNIME > Community Scripting > JupyterSettings.");
		
		isJupyterInstalled(jupyterLocation);
		
		// get jupyter path
		String jupyterDirString = getJupyterFolder(jupyterPreferences);
		if(jupyterDirString.isEmpty())
			throw new KnimeScriptingException("Path to jupyter folder is not set. Please configure under Preferences > KNIME > Community Scripting > JupyterSettings.");
		
		// 1) load notebook template and copy as tempfile for modification
		Path nbFile = null;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

		String timeString = LocalDateTime.now().format(formatter);
		try (InputStream utilsStream = getClass().getClassLoader().getResourceAsStream("/de/mpicbg/knime/scripting/python/scripts/template_notebook.ipynb")) {
			nbFile = Files.createTempFile(Paths.get(jupyterDirString), timeString + "_", ".ipynb");
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
		final String CLEANUP_PLACEHOLDER = "\"os.remove(r\\\"/path/to/tempfiles\\\")\"";
		
		final String kernelDisplayNamePlaceholder = "KERNEL_DISPLAY_NAME";
		final String kernelLanguagePlaceholder = "KERNEL_LANGUAGE";
		final String kernelNamePlaceholder = "KERNEL_NAME";
		
		/*
		 * supposed to look like this
		 * "kIn = read_csv(r\"***filename***\")",
		 * "kIn2 = read_csv(r\"***anotherfilename***\")"
		 */
		
		List<String> loadInputList = new LinkedList<String>();
		for(String inLabel : m_inPorts.keySet()) {
			File f = m_tempFiles.get(inLabel);
			if(f != null) {	
				
				String filename = f.getPath();
				if(isWindowsPlatform)
					filename  = filename.replace('\\', '/');
				
				String current = "\"" + inLabel + " = read_csv(r\\\"" + filename +"\\\")\\n\"";
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
				String filename = f.getPath();
				if(isWindowsPlatform)
					filename  = filename.replace('\\', '/');
				
				String current = "\"write_csv(r\\\"" + filename +"\\\"," + outLabel+ ")\\n\"";
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
		
		/*
		 * supposed to look like this
		 * "os.remove(r\"***filename***\")",
		 * "os.remove(r\"***otherfilename***\")"
		 */

		List<String> cleanupList = new LinkedList<String>();
		for(String tempFile : m_tempFiles.keySet()) {
			File f = m_tempFiles.get(tempFile);
			if(f != null) {	
				String filename = f.getPath();
				if(isWindowsPlatform)
					filename  = filename.replace('\\', '/');				
				
				String current = "\"" + "os.remove(r\\\"" + filename +"\\\")\\n\"";
				cleanupList.add(current);
			} else {
				throw new KnimeScriptingException("Failed to write script file");				
			}
		}
		String cleanup = String.join(",\n", cleanupList);
		
		Charset charset = StandardCharsets.UTF_8;
		String content;
		try {
			content = new String(Files.readAllBytes(nbFile), charset);
			content = content.replace(INPUT_PLACEHOLDER, loadInput);
			content = content.replace(OUTPUT_PLACEHOLDER, saveOutput);
			content = content.replace(CLEANUP_PLACEHOLDER, cleanup);
			content = content.replace(SCRIPT_PLACEHOLDER, script);	
			content = content.replace(kernelDisplayNamePlaceholder, jupyterKernel.getDisplayName());
			content = content.replace(kernelLanguagePlaceholder, jupyterKernel.getLanguage());
			content = content.replace(kernelNamePlaceholder, jupyterKernel.getName());
			Files.write(nbFile, content.getBytes(charset));
		} catch (IOException ioe) {
			throw new KnimeScriptingException("Failed to modify notebook format file: " + ioe.getMessage());
		}
		
		// 3) 
		launchNotebook(jupyterLocation, jupyterMode, nbFile);
	}

	/**
	 * return appropriate Jupyter kernel spec based on selected Python and preference settings
	 * 
	 * @param jupyterPreferences
	 * @param pythonVersion
	 * @return {@link JupyterKernelSpec}
	 */
	private JupyterKernelSpec getJupyterKernel(IPreferenceStore jupyterPreferences, String pythonVersion) {
		
		String kernelString = null;
		if(pythonVersion.equals(PythonPreferenceInitializer.PY2))		
			kernelString = jupyterPreferences.getString(ScriptingPreferenceInitializer.JUPYTER_KERNEL_PY2);
		if(pythonVersion.equals(PythonPreferenceInitializer.PY3))
			kernelString = jupyterPreferences.getString(ScriptingPreferenceInitializer.JUPYTER_KERNEL_PY3);
		
		if(kernelString == null) return null;
		
		String[] singleItems = kernelString.split(";");		
		int len = singleItems.length;
		
		if(len > 1) {
			int selectedIndex = Integer.parseInt(singleItems[0]);
			JupyterKernelSpec spec = JupyterKernelSpec.fromPrefString(singleItems[selectedIndex + 1]);
			if(!spec.getName().equals(JupyterKernelSpecsEditor.NO_SPEC))
				return spec;
		}	
		
		return null;
	}


	/**
	 * launch jupyter notebook with notebookfile
	 * 
	 * @param jupyterLocation
	 * @param nbFile
	 * 
	 * @throws KnimeScriptingException
	 */
	private void launchNotebook(String jupyterLocation, String jupyterMode, Path nbFile) throws KnimeScriptingException {
		
		assert python != null;
		String[] command;		
		
		try {
			if(isWindowsPlatform)
				command = new String[] {"powershell.exe", "-Command", jupyterLocation, jupyterMode, nbFile.toString()};
			else 
				command = new String[] {jupyterLocation, jupyterMode, nbFile.toString()};
			python.executeCommand(command, false);
			//python.executeCommand(new String[]{jupyterLocation, jupyterMode, nbFile.toString()}, false);
		} catch (Exception re) {
			throw new KnimeScriptingException("Failed while launching Jupyter: " + re.getMessage());
		}

	}

	/**
	 * check whether jupyter version can be obtained
	 * 
	 * @param jupyterLocation
	 * @return String with Jupyter version if successfull, exception thrown otherwise
	 * 
	 * @throws KnimeScriptingException
	 */
	private String isJupyterInstalled(String jupyterLocation) throws KnimeScriptingException {
		
		assert python != null;
		String outString = ""; 
		
		CommandOutput output;
		String[] command;		
		
		try {
			if(isWindowsPlatform)
				command = new String[] {"powershell.exe", "-Command", jupyterLocation, "notebook", "--version"};
			else 
				command = new String[] {jupyterLocation, "notebook", "--version"};
			output = python.executeCommand(command);	
			//output = python.executeCommand(new String[]{});
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
				if(value.equals("inf"))
					return new DoubleCell(Double.POSITIVE_INFINITY);
				if(value.equals("-inf"))
					return new DoubleCell(Double.NEGATIVE_INFINITY);
				
				double d = Double.parseDouble(value);
				return new DoubleCell(d);
			} catch (NumberFormatException nfe) {
				throw new KnimeScriptingException(nfe.getMessage());
			}
		}
		if(dType.equals(LongCellFactory.TYPE)) {
			try {
				long i = Long.parseLong(value);
				return new LongCell(i);
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
		if(dType.equals(DurationCellFactory.TYPE)) {
			try {
				Duration d = Duration.parse(value);
				return DurationCellFactory.create(d);
			} catch (DateTimeParseException dtpe) {
				return DataType.getMissingCell();
			}
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
		// is LocalDateTimeCell which has no public type available
		
		switch(cType) {
		case PY_TYPE_OBJECT: return StringCellFactory.TYPE;
		case PY_TYPE_BOOL: return BooleanCellFactory.TYPE;
		case PY_TYPE_FLOAT: return DoubleCellFactory.TYPE;
		case PY_TYPE_LONG: return LongCellFactory.TYPE;
		case PY_TYPE_DATETIME: return LocalDateTimeCellFactory.TYPE;
		case PY_TYPE_TIMEDELTA: return DurationCellFactory.TYPE;
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
		
		DataTableSpec tSpec = new DataTableSpec("Result from Python", cSpecArray);
		
		return tSpec;
	}

	/**
	 * write script to file for normal pushing of Python snippets
	 * 
	 * @throws KnimeScriptingException
	 */
	protected void prepareScriptFile() throws KnimeScriptingException {
		try(BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(getScriptFile(), true))) {
			prepareScript(scriptWriter, true, PythonInputMode.ignoreFlag());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new KnimeScriptingException("Failed to write script file: ", ioe.getMessage());
		}
	}

	/**
	 * @return list of input keys
	 */
	protected List<String> getInputKeys() {
		return m_inKeys;
	}

	/**
	 * sets list of input keys
	 * 
	 * @param keyList
	 */
	public void setInputKeys(List<String> keyList) {
		m_inKeys = keyList;
	}
	

}



package de.mpicbg.knime.scripting.r;


import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
>>>>>>> improve R transfer: transfer to R seems to work now, introduced chunks for big tables
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.data.RDataFrameContainer;
import de.mpicbg.knime.scripting.r.generic.RPortObject;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RUtils {

	// TODO: this needs to go to snippet nodes? or refactor with a nice name?
    public static final String SCRIPT_PROPERTY_DEFAULT = "rOut <- kIn";

    public static int MAX_FACTOR_LEVELS = 500;
    
    // constants defining the naming of variables used as KNIME input/output within R
    /** data-frame with input KNIME-table */
    public static final String VAR_RKNIME_IN = "knime.in";
    /** data-frame for output KNIME-table */
    public static final String VAR_RKNIME_OUT = "knime.out";
    /** list with input KNIME flow variables */
    public static final String VAR_RKNIME_FLOW_IN = "knime.flow.in";
    /** list for output KNIME flow variables */
    public static final String VAR_RKNIME_FLOW_OUT = "knime.flow.out";
    /** character vector for loading/saving used R packages */
    public static final String VAR_RKNIME_LIBS = "knime.loaded.libraries";
    /** KNIME color information TODO: how to represent in R? */
    public static final String VAR_RKNIME_COLOR = "knime.colors";
    /** KNIME input script */
    public static final String VAR_RKNIME_SCRIPT = "knime.script.in";
    
    /** enum for datatypes which can be pushed to R via R-serve */
    public enum RType { R_DOUBLE, R_LOGICAL, R_INT, R_STRING, R_FACTOR };
    
	public static RType getRType(DataType dataType, boolean hasDomainValues) {
		// numeric values (order is important)
		if(dataType.isCompatible(BooleanValue.class)) return RType.R_LOGICAL;
		if(dataType.isCompatible(IntValue.class)) return RType.R_INT;
		if(dataType.isCompatible(DoubleValue.class)) return RType.R_DOUBLE;
		
		// string
		if(dataType.isCompatible(StringValue.class)) {
			if(hasDomainValues) return RType.R_FACTOR;
			else return RType.R_STRING;
		}
		
		return null;
	}

	/**
	 * pushes one KNIME table to R in chunks
	 * @param exec				execution context
	 * @param bufTable			KNIME table
	 * @param colLimit			number of columns per chunk
	 * @param connection		R-connection
	 * @param parName			variable name in R
	 * @throws RserveException
	 * @throws REXPMismatchException
	 * @throws CanceledExecutionException
	 */
    public static void transferRDataContainer(ExecutionMonitor exec, BufferedDataTable bufTable, int colLimit,
    		RConnection connection, String parName) throws RserveException, REXPMismatchException, CanceledExecutionException {
    	
    	NodeLogger logger = NodeLogger.getLogger(RDataFrameContainer.class);

        DataTableSpec tSpec = bufTable.getDataTableSpec();
        int numRows = bufTable.getRowCount();
        int numCols = tSpec.getNumColumns();
        
        RDataFrameContainer rDFC = new RDataFrameContainer(numRows, numCols);

    	// iterate over table columns; find the columns which can be pushed
    	int chunkIdx = 0;
    	int chunkCounter = 0;
    	for(int colIdx = 0; colIdx < numCols; colIdx++) {
    		DataColumnSpec cSpec = tSpec.getColumnSpec(colIdx);
    		
    		String cName = cSpec.getName();
    		
    		//check if column type is supported, then add to columns to pass
    		RType type = getRType(cSpec.getType(), cSpec.getDomain().hasValues());
    		if(type != null) {
    			rDFC.addColumnSpec(cName, type, chunkIdx, colIdx);
    		
    			chunkCounter ++;
    			if(chunkCounter == colLimit || colIdx == (numCols - 1)) {
    				chunkIdx ++;
    				chunkCounter = 0;
    			}
    		} else {
    			logger.info("Ommit column " + cName + "; data type not supported");
    		}
    	}
    	
    	// iterate over the chunks
    	int nChunks = rDFC.getColumnChunks().size();
    	for(int chunk : rDFC.getColumnChunks()) {
    		
    		// set sub execution context for this chunk
    		ExecutionMonitor subExec = exec.createSubProgress(1.0/nChunks);
    		subExec.setMessage("Chunk" + (chunk+1));  	
    		
    		// initialize data vectors
    		rDFC.initDataVectors(chunk);    	
    		
    		// fill arrays with data
    		int rowIdx = 0;
    		for(DataRow row : bufTable) {
    			if(chunk == 0) rDFC.addRowKey(rowIdx, row.getKey().getString());
    			
    			subExec.checkCanceled();
    			subExec.setProgress(((double)rowIdx+1)/(double)numRows);
    			subExec.setMessage("Row " + rowIdx + "(chunk " + (chunk+1) + "/ " + nChunks + ")");
    			
    			rDFC.addRowData(row, rowIdx, chunk);
    			
    			rowIdx ++;
    		}
    		
    		rDFC.pushChunk(chunk, connection, parName, subExec);
    		rDFC.clearChunk(chunk);
    	}
    	
    	// if table has no columns, store row-keys only
    	if(nChunks == 0) {
    		int rowIdx = 0;
    		for(DataRow row : bufTable) {
    			rDFC.addRowKey(rowIdx, row.getKey().getString());
    			rowIdx ++;
    		}
    	}
    	
    	exec.setMessage("Create R data frame (cannot be cancelled)");
  	
    	rDFC.createDataFrame(parName, connection);
    	
    	exec.setMessage("Successful transfer to R");
    }


    /**
     * Ensures that the column names in the input table are valid data.frame attribute names.
     * Note: there is no column name which is not allowed in R data.frames (though maybe not recommended), unnecessary check?
     *
     * @throws RuntimeException if a column does not match the requirements as defined in the R language definition
     */
    private static void validateColumnNames(ExecutionContext exec, BufferedDataTable bufTable) throws CanceledExecutionException {
        for (DataColumnSpec columnSpec : bufTable.getDataTableSpec()) {
            exec.checkCanceled();

            String colName = columnSpec.getName();

            // fix column names which are not valid in R (that means containing invalid characters). cf. R-method 'make.names'
//            if (!colName.matches("[\\w]+[\\d\\w_. ]*")) {
            if (!colName.matches("[\\w]+.*")) {
                throw new RuntimeException("The column named '" + colName + "' is not compatible (=start with somethine A-z) with R and needs to be renamed");
            }
        }
    }

    // TODO: replace it
    public static BufferedDataTable convert2DataTable(ExecutionContext exec, REXP rexp, Map<String, DataType> typeMapping) {
        try {
            RList rList = rexp.asList();


            // create attributes
            DataColumnSpec[] colSpecs = new DataColumnSpec[rList.keySet().size()];
            Map<DataColumnSpec, REXPVector> columnData = new HashMap<DataColumnSpec, REXPVector>();
            Map<DataColumnSpec, Integer> colIndices = new HashMap<DataColumnSpec, Integer>();

            ArrayList colKeys = new ArrayList(Arrays.asList(rList.keys()));

//            long timeBefore = System.currentTimeMillis();


            for (int attrCounter = 0; attrCounter < colKeys.size(); attrCounter++) {
                Object columnKey = colKeys.get(attrCounter);
                REXPVector column = (REXPVector) rList.get(columnKey);

                assert columnKey instanceof String : " key is not a string";
                String columnName = columnKey.toString();


                if (column.isFactor()) {
                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec();

                } else if (column.isNumeric()) {
                    DataType numColType = DoubleCell.TYPE;
                    if (typeMapping != null && typeMapping.containsKey(columnName)) {
                        numColType = typeMapping.get(columnName);
                    }

                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, numColType).createSpec();

                } else if (column.isString()) {
                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec();

                } else if (column.isLogical()) {
                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, IntCell.TYPE).createSpec();
                    //                    colSpecs[specCounter] = new DataColumnSpecCreator(columnName, LogicalCell.TYPE).createSpec();

                } else {
                    throw new RuntimeException("column type not supported");
                }

                DataColumnSpec curSpecs = colSpecs[attrCounter];

                columnData.put(curSpecs, column);
                colIndices.put(curSpecs, attrCounter);
            }

//            NodeLogger.getLogger(RUtils.class).warn(rexp.toString() + "- Time between (R->knime) 1: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


            // create examples
            DataTableSpec outputSpec = new DataTableSpec(colSpecs);
            BufferedDataContainer container = exec.createDataContainer(outputSpec);
            int numExamples = columnData.isEmpty() ? -1 : columnData.values().iterator().next().length();

            DataCell[][] cells = new DataCell[numExamples][colIndices.size()];

            List<DataColumnSpec> domainSpecs = new ArrayList<DataColumnSpec>();

            for (DataColumnSpec colSpec : colSpecs) {
                REXPVector curColumn = columnData.get(colSpec);
                int colIndex = colIndices.get(colSpec);

                DataColumnSpecCreator specCreator = new DataColumnSpecCreator(colSpec.getName(), colSpec.getType());
                boolean[] isNA = curColumn.isNA();


                if (curColumn.isString() || curColumn.isFactor()) {
                    String[] stringColumn = curColumn.asStrings();

                    LinkedHashSet<DataCell> domain = new LinkedHashSet<DataCell>();

                    for (int i = 0; i < numExamples; i++) {
                        if (isNA[i] || stringColumn[i].equals(RDataFrameContainer.NA_VAL_FOR_R)) {
                            cells[i][colIndex] = DataType.getMissingCell();

                        } else {
                            StringCell stringCell = new StringCell(stringColumn[i]);
                            updateDomain(domain, stringCell);

                            cells[i][colIndex] = stringCell;
                        }
                    }

                    if (domain.size() < MAX_FACTOR_LEVELS) {
                        specCreator.setDomain(new DataColumnDomainCreator(domain).createDomain());
                    }

                } else if (colSpec.getType().isCompatible(IntValue.class) || curColumn.isLogical()) {    // there's no boolean-cell-type in knime, thus we use int
                    int[] intColumn = curColumn.asIntegers();

                    for (int i = 0; i < numExamples; i++) {
                        cells[i][colIndex] = isNA[i] ? DataType.getMissingCell() : new IntCell(intColumn[i]);
                    }

                } else if (curColumn.isNumeric()) {
                    double[] doubleColumn = curColumn.asDoubles();

                    for (int i = 0; i < numExamples; i++) {
                        cells[i][colIndex] = isNA[i] ? DataType.getMissingCell() : new DoubleCell(doubleColumn[i]);
                    }


                } else {
                    throw new RuntimeException("Unexpected type data-frame that has been received from R: " + colSpec.getName());
                }

                //  maybe we have to treat other types here

                domainSpecs.add(specCreator.createSpec());
            }


//            NodeLogger.getLogger(RUtils.class).warn(rexp.toString() + "- Time danach (R->knime) 2: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


            for (int i = 0; i < numExamples; i++) {
                RowKey key = new RowKey("" + i);

                DataRow row = new DefaultRow(key, cells[i]);
                container.addRowToTable(row);
            }

//            NodeLogger.getLogger(RUtils.class).warn(rexp.toString() + "- Time danach (R->knime) 3: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


//            exec.checkCanceled();
//            exec.setProgress(i / (double)m_count.getIntValue(), "Adding row " + i);

//        // once we are done, we close the container and return its table
            container.close();

            DataTableSpec domainDataTableSpec = new DataTableSpec(domainSpecs.toArray(new DataColumnSpec[domainSpecs.size()]));

            return exec.createSpecReplacerTable(container.getTable(), domainDataTableSpec);
        } catch (REXPMismatchException e) {
            throw new RuntimeException(e);
        }

    }


    private static void updateDomain(LinkedHashSet<DataCell> domain, StringCell stringCell) {
        if (!stringCell.isMissing() && domain.size() < MAX_FACTOR_LEVELS) {
            domain.add(stringCell);
        }
    }


    public static String chop(String str) {
        if (str == null) {
            return null;
        }
        int strLen = str.length();
        if (strLen < 2) {
            return "";
        }
        int lastIdx = strLen - 1;
        String ret = str.substring(0, lastIdx);
        char last = str.charAt(lastIdx);
        if (last == '\n') {
            if (ret.charAt(lastIdx - 1) == '\r') {
                return ret.substring(0, lastIdx - 1);
            }
        }
        return ret;
    }


    public static RConnection createConnection() throws KnimeScriptingException {

        String host = getHost();
        int port = getPort();
        
        try {
            return new RConnection(host, port);
        } catch (RserveException e) {
        	e.printStackTrace();
        	throw new KnimeScriptingException("Could not connect to R. Probably, the R-Server is not running.\nHere's what you need to do:\n 1) Check what R-server-host is configured in your Knime preferences.\n 2) If your host is set to be 'localhost' start R and run the following command\n library(Rserve); Rserve(args = \"--vanilla\")");
        }
    }


    public static String getHost() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_HOST);
    }


    public static int getPort() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getInt(RPreferenceInitializer.R_PORT);
    }

    public static String fixEncoding(String stringValue) {
        try {
            return new String(stringValue.getBytes("UTF-8"), "UTF-8").replace("\r", "");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not change encoding of r-command to UTF8");
        }
    }


    public static void loadGenericInputs(Map<String, File> varFileMapping, RConnection connection) throws RserveException, REXPMismatchException, IOException {

        for (String varName : varFileMapping.keySet()) {

            // summary of happens below: create a copy of the current workspace file in r, load it in r and rename the content to match the current nodes connectivity pattern

            // mirror the ws-file on the server side
            connection.voidEval("tmpwfile <- 'tempRws';");
            connection.voidEval("file.create(tmpwfile);");
            File serverWSFile = new File(connection.eval("tmpwfile").asString());

            writeFile(varFileMapping.get(varName), serverWSFile, connection);

            // load the workspace on the server side
            connection.voidEval("load(tmpwfile);");

            // rename the input structure to match the current environment ones

            // rename kIn if file is still using the old naming scheme
            List<String> wsVariableNames = Arrays.asList(connection.eval("ls()").asStrings());

            if (wsVariableNames.contains("R")) {
                // rename the old R-variable to the new format
                connection.voidEval(RSnippetNodeModel.R_OUTVAR_BASE_NAME + " <- R;");
            }

            // if its the plotting node the input is still names kIn, so just rename if there no such variable already present
            if (!wsVariableNames.contains(varName)) {
                connection.eval(varName + " <- " + RSnippetNodeModel.R_OUTVAR_BASE_NAME + ";");

                // do some cleanup
                connection.voidEval("rm(" + RSnippetNodeModel.R_OUTVAR_BASE_NAME + ");");
            }
        }
    }


    private static void writeFile(File wsFile, File serverWSFile, RConnection connection) {
        try {
            assert wsFile.isFile();

            InputStream is = new BufferedInputStream(new FileInputStream(wsFile));
            OutputStream os = new BufferedOutputStream(connection.createFile(serverWSFile.getPath()));

            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }

            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void saveToLocalFile(File rWorkspaceFile, RConnection connection, String host, String... objectNames) 
    		throws RserveException, IOException, REXPMismatchException, REngineException, KnimeScriptingException {

        connection.voidEval("tmpwfile = tempfile('tempRws');");
        
        //check whether named objects exist in R workspace
        for(int i = 0; i < objectNames.length; i++) {
        	String evalExistense = "exists(\"" + objectNames[i] + "\")";
        	if(((REXPLogical)connection.eval(evalExistense)).isFALSE()[0])
        		throw new KnimeScriptingException("Try to save object " + objectNames[i] + ". Object does not exist in R workspace.");
        }

        String allParams = Arrays.toString(objectNames).replace("[", "").replace("]", "").replace(" ", "");
        connection.voidEval("save(" + allParams + ", file=tmpwfile); ");


        // 2) transfer the file to the local computer if necessary
//        logger.info("Transferring workspace-file to localhost ...");


        // to furthe improve performance we simply copy the file on a local host
        if (host != null && host.equals("localhost")) {
            File localRWSFile = new File(connection.eval("tmpwfile").asString());
             copyFile(localRWSFile, rWorkspaceFile);

        } else {
            // create the local file and transfer the workspace file from the rserver

            if (rWorkspaceFile.isFile()) {
                rWorkspaceFile.delete();
            }

            rWorkspaceFile.createNewFile();

            REXP xp = connection.parseAndEval("r=readBin(tmpwfile,'raw'," + 1E7 + "); unlink(tmpwfile); r");
            FileOutputStream oo = new FileOutputStream(rWorkspaceFile);
            oo.write(xp.asBytes());
            oo.close();
        }

        //remove the temporary workspace file
        connection.voidEval("rm(tmpwfile);");

    }


    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * push all incoming KNIME-objects to R
     * @param inObjects
     * @param connection
     * @param exec
     * @return
     * @throws KnimeScriptingException
     */
    public static Map<String, Object> pushToR(PortObject[] inObjects, RConnection connection, ExecutionMonitor exec) throws KnimeScriptingException {
        Map<String, Object> inputMapping = createPortMapping(inObjects);
        
        double nInput = inputMapping.size();
        exec.setMessage("Transfer to R");

        // first push the generic inputs
        Map<String, File> genPortMapping = getGenericPorts(inputMapping);
        double nGenerics = genPortMapping.size();
        try {
            if(genPortMapping.size() > 0) RUtils.loadGenericInputs(genPortMapping, connection);
        } catch (Throwable e) {
            throw new KnimeScriptingException("Failed to convert generic node inputs into r workspace variables: " + e);
        }

        // second, push the table inputs
        Map<String, BufferedDataTable> tablePortMapping = getDataTablePorts(inputMapping);
        double nTables = tablePortMapping.size();
        try {
            if(tablePortMapping.size() > 0) RUtils.loadTableInputs(connection, tablePortMapping, exec.createSubProgress(nTables/nInput));
        } catch (Throwable e) {
            throw new KnimeScriptingException("Failed to convert table node inputs into r workspace variables: " + e);
        }
        return inputMapping;
    }

    /**
     * transfers KNIME tables to R
     * @param connection
     * @param tableMapping
     * @param exec
     * @throws REXPMismatchException
     * @throws RserveException
     * @throws CanceledExecutionException
     */
    private static void loadTableInputs(RConnection connection, Map<String, BufferedDataTable> tableMapping, ExecutionMonitor exec) throws REXPMismatchException, RserveException, CanceledExecutionException {
    	//TODO: add chunk-size as node setting
    	int chunksize = 4;

    	// transfer KNIME-tables to R
        for (String varName : tableMapping.keySet()) {
            BufferedDataTable input = tableMapping.get(varName);

            if (input == null) {
                throw new RuntimeException("null tables are not allowed in the table input mapping");
            }

            transferRDataContainer(exec.createSubProgress(1.0/tableMapping.size()), input, chunksize, connection, varName);           
        }
    }

    private static Map<String, BufferedDataTable> getDataTablePorts(Map<String, Object> inputMapping) {
        TreeMap<String, BufferedDataTable> tablePortMapping = new TreeMap<String, BufferedDataTable>();

        for (String varName : inputMapping.keySet()) {
            Object curValue = inputMapping.get(varName);
            if (curValue instanceof BufferedDataTable) {

                tablePortMapping.put(varName, (BufferedDataTable) curValue);
            }

        }

        return tablePortMapping;
    }

    /**
     * delivers a map containing generic input ports mapped to an R variable name
     * @param inputMapping
     * @return map with generic input ports only
     */
    private static Map<String, File> getGenericPorts(Map<String, Object> inputMapping) {
        TreeMap<String, File> genericSubMapping = new TreeMap<String, File>();

        for (String varName : inputMapping.keySet()) {
            Object curValue = inputMapping.get(varName);
            if (curValue instanceof File) {

                genericSubMapping.put(varName, (File) curValue);
            }

        }

        return genericSubMapping;
    }


    /**
     * The values of the map are either files or (buffered)data tables.
     * @param inObjects
     * @return
     */
    public static Map<String, Object> createPortMapping(PortObject[] inObjects) {

        Map<String, Object> portVarMapping = new TreeMap<String, Object>();

        // number of non-null input port objects
        int numConnectedInputs = 0;
        for (PortObject inport : inObjects) {
            if (inport != null) {
                numConnectedInputs++;
            }
        }

        // create naming of input variables for R; e.g. kIn, or kIn1
        // map port objects to variable name
        // TODO: creates input variable for null-objects, is there any reason?
        for (int i = 0; i < inObjects.length; i++) {
        	String RVariableName = RSnippetNodeModel.R_INVAR_BASE_NAME + (numConnectedInputs > 1 ? (i + 1) : "");
            
            PortObject inport = inObjects[i];
            if (inport == null) {
                continue;
            }

            if (inport instanceof RPortObject) {
                portVarMapping.put(RVariableName, ((RPortObject) inport).getFile());
            } else if (inport instanceof BufferedDataTable) {
                portVarMapping.put(RVariableName, inport);
            } else {
                throw new RuntimeException("Unexpected port type: " + inport);
            }
        }

        return portVarMapping;
    }


    public static String supportOldVarNames(String script) throws RserveException {
        if (script.contains("(R)") ||
                script.contains("R[") ||
                script.contains("R$") ||
                script.contains("<- R") ||
                script.contains("R <-")) {

            String renamePrefix = "R <- " + RSnippetNodeModel.R_INVAR_BASE_NAME;
            String renameSuffix = RSnippetNodeModel.R_OUTVAR_BASE_NAME + " <- R";

            return renamePrefix + "; " + script + "\n" + renameSuffix;
        }

        return script;
    }


    /**
     * use 'evaluate' package to execute the script and keep input+output+errors+warnings
     * 
     * @param fixedScript 	the R script
     * @param connection	the connection to the R server
     * @param logger		the node-logger to push warnings
     * 
     * @return	R list containing input, output, plots, (errors - not returned; throws exception instead) and warnings
     * 
     * @throws RserveException
     * @throws KnimeScriptingException
     * @throws REXPMismatchException
     */
	public static REXPGenericVector evaluateScript(String fixedScript, RConnection connection) 
			throws RserveException, KnimeScriptingException, REXPMismatchException {
		
		// use 'evaluate' package to capture input+output+warnings+error
    	// syntax errors are captured with try
    	REXP r;
    	
    	// try to load evaluate package
    	r = connection.eval("try(library(\"evaluate\"))");
    	if (r.inherits("try-error")) 
    		throw new KnimeScriptingException("Package 'evaluate' could not be loaded. \nTo run the script without, please turn off 'Evaluate script' in the node configuration dialog / preference settings?.");
    
    	// try to evaluate script (fails with syntax errors)
    	connection.assign(VAR_RKNIME_SCRIPT, fixedScript);
    	r = connection.eval("knime.eval.obj <- evaluate("+ VAR_RKNIME_SCRIPT + ", new_device = FALSE)");
    	
    	// evaluation succeeded
    	// check for errors
    	int[] errIdx = ((REXPInteger) connection.eval("which(sapply(knime.eval.obj, inherits, \"error\") == TRUE, arr.ind = TRUE)")).asIntegers();
    	if(errIdx.length > 0) {
   
    		String firstError = "Error " + "(1/" + errIdx.length + "): ";
    		REXPString error = (REXPString) connection.eval("knime.eval.obj[[" + errIdx[0] + "]]$message");
    		firstError = firstError + error.asString() + "\n\tSee R-console view for further details";

    		throw new KnimeScriptingException(firstError);
    	}
    	
    	return (REXPGenericVector) (connection.eval("knime.eval.obj"));
	}
	
	public static void evalScript(RConnection connection, String fixedScript)
			throws RserveException, KnimeScriptingException,
			REXPMismatchException {
		
		REXP out;
		// evaluate script
		out = connection.eval("try({" + fixedScript + "}, silent = TRUE)");
		if( out.inherits("try-error"))
			throw new KnimeScriptingException("Error : " + out.asString());
	}


	public static void parseScript(RConnection connection, String fixedScript)
			throws RserveException, KnimeScriptingException,
			REXPMismatchException {
		REXP out;
		String rScriptVar = RUtils.VAR_RKNIME_SCRIPT;
		connection.assign(rScriptVar, fixedScript);
		// parse script
		out = connection.eval("try(parse(text=" + rScriptVar + "), silent = TRUE)");
		if( out.inherits("try-error"))
			throw new KnimeScriptingException("Syntax error: " + out.asString());
	}


	public static BufferedDataTable convert2DataTable(ExecutionContext exec,
			REXP out, String[] rowNames, Map<String, DataType> typeMapping) {
		// TODO use row names for output table
		return convert2DataTable(exec, out, typeMapping);
	}


	public static Image createImage(RConnection connection, String script, int width, int height, String device) 
			throws REngineException, RserveException, REXPMismatchException, KnimeScriptingException {
	
	    	// check preferences
	    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);
	    	
	        // LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
	        script = supportOldVarNames(script);
	
	        String tempFileName = "rmPlotFile." + device;
	
	        String deviceArgs = device.equals("jpeg") ? "quality=97," : "";
	        REXP xp = connection.eval("try(" + device + "('" + tempFileName + "'," + deviceArgs + " width = " + width + ", height = " + height + "))");
	
	        if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
	            // this is analogous to 'warnings', but for us it's sufficient to get just the 1st warning
	            REXP w = connection.eval("if (exists('last.warning') && length(last.warning)>0) names(last.warning)[1] else 0");
	            if (w.isString()) System.err.println(w.asString());
	            throw new KnimeScriptingException("Can't open " + device + " graphics device:\n" + xp.asString());
	        }
	
	        // ok, so the device should be fine - let's plot - replace this by any plotting code you desire ...
	        String preparedScript = fixEncoding(script);
	        RUtils.parseScript(connection, preparedScript);
	
	        if(useEvaluate) {
	        	// parse and run script
	        	// evaluation list, can be used to create a console view
	        	evaluateScript(preparedScript, connection);
	
	        } else {
	        	// parse and run script
	        	evalScript(connection, preparedScript);     	
	        }
	
	        // close the image
	        connection.eval("dev.off();");
	        // check if the plot file has been written
	        int xpInt = connection.eval("file.access('" + tempFileName + "',0)").asInteger();
	        if(xpInt == -1) throw new KnimeScriptingException("Plot could not be created. Please check your script");
	
	        // we limit the file size to 1MB which should be sufficient and we delete the file as well
	        xp = connection.eval("try({ binImage <- readBin('" + tempFileName + "','raw',2024*2024); unlink('" + tempFileName + "'); binImage })");
	        
	        if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
	            throw new KnimeScriptingException(xp.asString());
	        }
	
	        // now this is pretty boring AWT stuff - create an image from the data and display it ...
	        return Toolkit.getDefaultToolkit().createImage(xp.asBytes());
	    }


	/**
	 * assumes in R workspace an objects resulting from 'evaluate'-function call
	 * retrieves a list of error messages from this object
	 * 
	 * @param connection
	 * @return list with error messages
	 * @throws RserveException
	 * @throws REXPMismatchException
	 */
	public static ArrayList<String> checkForErrors(RConnection connection) 
			throws RserveException, REXPMismatchException {
		
		ArrayList<String> errorMessages = new ArrayList<String>();
		if (((REXPLogical) connection.eval("exists(\"knime.eval.obj\")")).isFALSE()[0])
			return errorMessages;
		
		// check for errors
    	int[] errIdx = ((REXPInteger) connection.eval("which(sapply(knime.eval.obj, inherits, \"error\") == TRUE, arr.ind = TRUE)")).asIntegers();
    	if(errIdx.length > 0) {
    		for(int i=0; i < errIdx.length; i++){
    			REXPString error = (REXPString) connection.eval("knime.eval.obj[[" + errIdx[i] + "]]$message");
    			errorMessages.add(error.asString());
    		}
    	}
    	return errorMessages;
	}

	/**
	 * assumes in R workspace an objects resulting from 'evaluate'-function call
	 * retrieves a list of error messages from this object
	 * 
	 * @param connection
	 * @return list with warning messages
	 * @throws RserveException
	 * @throws REXPMismatchException
	 */
	public static ArrayList<String> checkForWarnings(RConnection connection) 
			throws RserveException, REXPMismatchException {
		
		ArrayList<String> warnMessages = new ArrayList<String>();
		if (((REXPLogical) connection.eval("exists(\"knime.eval.obj\")")).isFALSE()[0])
			return warnMessages;
		
		//check for warnings
    	int[] warnIdx = ((REXPInteger) connection.eval("which(sapply(knime.eval.obj, inherits, \"warning\") == TRUE, arr.ind = TRUE)")).asIntegers();
    	if(warnIdx.length > 0) {
    		for(int i=0; i < warnIdx.length; i++){
    			String singleWarning;
    			REXPString warn = (REXPString) connection.eval("deparse(knime.eval.obj[[" + warnIdx[i] + "]]$call)");
    			singleWarning = warn.asString() + " : ";
    			warn = (REXPString) connection.eval("knime.eval.obj[[" + warnIdx[i] + "]]$message");
    			singleWarning = singleWarning + warn.asString() + "\n";
    			warnMessages.add(singleWarning);
    		}
    	}
    	return warnMessages;
	}
	

	/**
	 * run the actual external call to open R with Knime data
	 * @param workspaceFile
	 * @param script
	 * @throws IOException
	 */
    public static void openWSFileInR(File workspaceFile, String script) throws IOException {
        IPreferenceStore prefStore = R4KnimeBundleActivator.getDefault().getPreferenceStore();
        String rExecutable = prefStore.getString(RPreferenceInitializer.LOCAL_R_PATH);

        // 3) spawn a new R process
        if (Utils.isWindowsPlatform()) {
            Runtime.getRuntime().exec(rExecutable + " " + workspaceFile.getAbsolutePath());
        } else if (Utils.isMacOSPlatform()) {

            Runtime.getRuntime().exec("open -n -a " + rExecutable + " " + workspaceFile.getAbsolutePath());

        } else { // linux and the rest of the world
            Runtime.getRuntime().exec(rExecutable + " " + workspaceFile.getAbsolutePath());
        }

        // copy the script in the clipboard
        if (!script.isEmpty()) {
            StringSelection data = new StringSelection(script);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(data, data);
        }
    }
    
    /**
     * Input data is transfered to R and workspace saved in tempfile
     * @param inData
     * @param exec
     * @param rawScript
     * @param logger
     * @throws KnimeScriptingException
     * @throws REXPMismatchException
     * @throws IOException
     * @throws REngineException
     */
    public static void openInR(PortObject[] inData, ExecutionContext exec, String rawScript, NodeLogger logger) 
    		throws KnimeScriptingException, REXPMismatchException, IOException, REngineException {

    	logger.info("Creating R-connection");
    	RConnection connection = RUtils.createConnection();

    	// 1) convert exampleSet into data-frame and put into the r-workspace
    	logger.info("Pushing inputs to R...");

    	Map<String, Object> pushTable = RUtils.pushToR(inData, connection, exec);

    	// save the work-space to a temporary file and open R
    	String allParams = pushTable.keySet().toString().replace("[", "").replace("]", "").replace(" ", "");

    	connection.voidEval("tmpwfile = tempfile('openinrnode', fileext='.RData');");
    	
    	// enable to save nothing in workspace files for source nodes
    	if(pushTable.size() > 0)
    		connection.voidEval("save(" + allParams + ", file=tmpwfile); ");
    	else
    		connection.voidEval("save(file=tmpwfile); ");

    	// 2) transfer the file to the local computer if necessary
    	logger.info("Transferring workspace-file to localhost ...");

    	File workspaceFile = null;
    	if (RUtils.getHost().equals("localhost")) {
    		workspaceFile = new File(connection.eval("tmpwfile").asString());
    	} else {
    		// create another local file  and transfer the workspace file from the rserver
    		workspaceFile = File.createTempFile("rplugin", ".RData");
    		workspaceFile.deleteOnExit();

    		REXP xp = connection.parseAndEval("r=readBin(tmpwfile,'raw',file.info(tmpwfile)$size); unlink(tmpwfile); r");
    		FileOutputStream oo = new FileOutputStream(workspaceFile);
    		oo.write(xp.asBytes());
    		oo.close();
    	}
    	connection.voidEval("rm(list = ls(all = TRUE));");
    	connection.close();

    	logger.info("Spawning R-instance ...");
    	RUtils.openWSFileInR(workspaceFile, rawScript);           
    }




	public static REXP createDataFrameNoRownames(RList l) throws REXPMismatchException {
    	if (l == null) throw new NullPointerException("data frame (cannot be null)");
    	if (!(l.at(0) instanceof REXPVector)) throw new REXPMismatchException(new REXPList(l), "data frame (contents must be vectors)");
    	return
    			new REXPGenericVector(l);
    }
	
	
	public static void main(String[] args) {
		System.out.println("this is a test");
		
		int n = 20000;
		
		RList rList = new RList(3, true);
		
		String[] rowNames = new String[n];
		double[] data = new double[n];
		for(int i = 0; i < n; i++) {
			data[i] = i * Math.E;
			String rn = i + "#this is my row key a very long one";
			rowNames[i] = rn.substring(0, 30);
		}

        rList.put("Intensity_MeanIntensity_GFP_Cytoplasmic0", new REXPDouble(data));
        rList.put("Intensity_MeanIntensity_GFP_Cytoplasmic1", new REXPDouble(data));
        rList.put("testdata3", new REXPString(rowNames));

        String host = "localhost";
        int port = 6311;  
        
        try {
        	//REXP df = createDataFrame(rList, rowNames);
        	REXP df = createDataFrameNoRownames(rList);
			RConnection con = new RConnection(host, port);
            con.assign("kIn", df);
            con.assign("rownames", new REXPString(rowNames));
            con.eval("attr(kIn, \"row.names\") <- .set_row_names(length(kIn[[1]])); class(kIn) <- \"data.frame\"; rownames(kIn) <- rownames");
            String[] colnames = con.eval("names(kIn)").asStrings();
            for(String s : colnames)
            	System.out.println("Column name: " + s);
            String[] rownames = con.eval("rownames(kIn)").asStrings();
            for(String s : rownames)
            	System.out.println(s + ", ");
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
				
        System.out.println("done");
		
	}
}
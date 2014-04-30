package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.knutils.AttributeUtils;
import de.mpicbg.knime.scripting.r.generic.RPortObject;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.knime.core.data.*;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RUtils {

    public static final String SCRIPT_PROPERTY_DEFAULT = "rOut <- kIn;";

    public static int MAX_FACTOR_LEVELS = 500;
    public static final String NA_VAL_FOR_R = "NA";


    public static RList convert2RList(ExecutionContext exec, BufferedDataTable bufTable) throws RserveException, REXPMismatchException, CanceledExecutionException {
//        long timeBefore = System.currentTimeMillis();


        DataTableSpec tableSpecs = bufTable.getDataTableSpec();
        List<String> includeColumns = AttributeUtils.toStringList(AttributeUtils.convert(tableSpecs));

        validateColumnNames(exec, bufTable);

        List<Object> columArrays = new ArrayList<Object>();
        Map<Integer, DataType> converters = new TreeMap<Integer, DataType>();

        int numRows = bufTable.getRowCount();

        // 1) initialize the arrays
        for (int i = 0; i < tableSpecs.getNumColumns(); i++) {

            DataColumnSpec colSpec = tableSpecs.getColumnSpec(i);
            DataType colType = colSpec.getType();

            if (!includeColumns.contains(colSpec.getName())) {
                continue;
            }

            converters.put(i, colType);

            if (colType.equals(DoubleCell.TYPE)) {
                columArrays.add(new double[numRows]);

            } else if (colType.equals(StringCell.TYPE) || colType.equals(DateAndTimeCell.TYPE)) {
                columArrays.add(new String[numRows]);

            } else if (colType.equals(IntCell.TYPE)) {
                columArrays.add(new int[numRows]);

            } else {
                throw new RuntimeException("value type of attribute '" + colSpec.getName() + "' not supported");
            }
        }

//        NodeLogger.getLogger(RUtils.class).warn(bufTable.hashCode() + "- Time danach (knime->R) 1: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


        // 2) Perform the actual table conversion: loop ONCE (for performance reasons) over the table and populate the array for R
        int rowCounter = 0;
        for (DataRow dataRow : bufTable) {
            exec.checkCanceled();

            for (Integer colIndex : converters.keySet()) {
                DataType colType = converters.get(colIndex);

                if (colType.equals(DoubleCell.TYPE)) {
                    double[] numColumn = (double[]) columArrays.get(colIndex);
                    DataCell dataCell = dataRow.getCell(colIndex);

                    if (dataCell instanceof DoubleCell) {
                        double value = ((DoubleCell) dataCell).getDoubleValue();
                        numColumn[rowCounter] = value;

                    } else if (dataCell instanceof IntCell) {  // this may happen after two table with matching column names but differeig types are concatenated.
                        int intValue = ((IntCell) dataCell).getIntValue();
                        numColumn[rowCounter] = intValue;

                    } else if (dataCell.isMissing()) {
                        numColumn[rowCounter] = REXPDouble.NA;

                    } else {
                        numColumn[rowCounter] = Double.NaN;
                    }

                } else if (colType.equals(StringCell.TYPE) || colType.equals(DateAndTimeCell.TYPE)) {
                    String[] strColumn = (String[]) columArrays.get(colIndex);
                    DataCell dataCell = dataRow.getCell(colIndex);

                    if (dataCell instanceof StringCell) {
                        String stringValue = ((StringCell) dataCell).getStringValue();

                        // TODO REMOVE THIS HACK AS SOON AS https://www.rforge.net/bugzilla/show_bug.cgi?id=194 IS FIXED
                        stringValue = stringValue.trim().isEmpty() ? NA_VAL_FOR_R : stringValue;
                        // TODO REMOVE THIS HACK AS SOON AS https://www.rforge.net/bugzilla/show_bug.cgi?id=194 IS FIXED

                        strColumn[rowCounter] = stringValue;

                    } else if (dataCell instanceof DateAndTimeCell) {
                        String dateValue = ((DateAndTimeCell) dataCell).getStringValue();
                        strColumn[rowCounter] = dateValue;

                    } else if (dataCell.isMissing()) {
                        strColumn[rowCounter] = NA_VAL_FOR_R;
                    }

                } else if (colType.equals(IntCell.TYPE)) {
                    int[] numColumn = (int[]) columArrays.get(colIndex);

                    DataCell dataCell = dataRow.getCell(colIndex);
                    if (dataCell instanceof IntCell) {
                        int intValue = ((IntCell) dataCell).getIntValue();
                        numColumn[rowCounter] = intValue;

                    } else if (dataCell instanceof DoubleCell) { // this may happen after two table with matching column names but differeig types are concatenated.
                        double doubleValue = ((DoubleCell) dataCell).getDoubleValue();
                        numColumn[rowCounter] = (int) Math.round(doubleValue);

                    } else if (dataCell.isMissing()) {
                        numColumn[rowCounter] = REXPInteger.NA;

                    } else {
                        //http://cran.r-project.org/doc/manuals/R-data.html
                        numColumn[rowCounter] = (int) Double.NaN;
                    }
                }
            }

            rowCounter++;
        }

//        NodeLogger.getLogger(RUtils.class).warn(bufTable.hashCode() + "- Time danach (knime->R) 2: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


        // 3) create the REXP (which is a list of columns
        RList rList = new RList(bufTable.getDataTableSpec().getNumColumns(), true);

        for (Integer colIndex : converters.keySet()) {
            DataType colType = converters.get(colIndex);
            String colName = tableSpecs.getColumnSpec(colIndex).getName();

            if (colType.equals(DoubleCell.TYPE)) {
                rList.put(colName, new REXPDouble((double[]) columArrays.get(colIndex)));

            } else if (colType.equals(StringCell.TYPE) || colType.equals(DateAndTimeCell.TYPE)) {
                rList.put(colName, new REXPString((String[]) columArrays.get(colIndex)));

            } else if (colType.equals(IntCell.TYPE)) {
                rList.put(colName, new REXPInteger((int[]) columArrays.get(colIndex)));
            }
        }

//        NodeLogger.getLogger(RUtils.class).warn(bufTable.hashCode() + "- Time danach (knime->R) 3: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


        return rList;
    }


    /**
     * Ensures that the column names in the input table are valid data.frame attribute names.
     *
     * @throws RuntimeException if a column does not match the requirements as defined in the R language definition
     */
    private static void validateColumnNames(ExecutionContext exec, BufferedDataTable bufTable) throws CanceledExecutionException {
        for (DataColumnSpec columnSpec : bufTable.getDataTableSpec()) {
            exec.checkCanceled();

            String colName = columnSpec.getName();

            // fix colum names which are not valid in R (that means containing invalid characters). cf. R-method 'make.names'
//            if (!colName.matches("[\\w]+[\\d\\w_. ]*")) {
            if (!colName.matches("[\\w]+.*")) {
                throw new RuntimeException("The column named '" + colName + "' is not compatible (=start with somethine A-z) with R and needs to be renamed");
            }
        }
    }


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
                        if (isNA[i] || stringColumn[i].equals(NA_VAL_FOR_R)) {
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


    public static RConnection createConnection() {

        String host = getHost();
        int port = getPort();

        try {
            return new RConnection(host, port);
        } catch (RserveException e) {
            throw new RuntimeException("Could not connect to R. Probably, the R-Server is not running.\nHere's what you need to do:\n 1) Check what R-server-host is configured in your Knime preferences.\n 2) If your host is set to be 'localhost' start R and run the following command\n library(Rserve); Rserve(args = \"--vanilla\")");

        }
    }


    public static String getHost() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_HOST);
    }


    public static int getPort() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getInt(RPreferenceInitializer.R_PORT);
    }


    public static void evalScript(RConnection connection, String preparedScript) {
        try {

            connection.parseAndEval(preparedScript);

        } catch (Throwable e) {
            throw new RuntimeException("Error while executing script: " + preparedScript + "\n The error was: " + e + "\n It's likely that the syntax of your script is not correct.", e);
        }
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


    public static void saveToLocalFile(File rWorkspaceFile, RConnection connection, String host, String... objectNames) throws RserveException, IOException, REXPMismatchException, REngineException {

        connection.voidEval("tmpwfile = tempfile('tempRws');");

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

    // a small testing environment to test r-communication


    public static Map<String, Object> pushToR(PortObject[] inObjects, RConnection connection, ExecutionContext exec) {
        Map<String, Object> inputMapping = createPortMapping(inObjects);

        // first push the generic inputs
        Map<String, File> genPortMapping = getGenericPorts(inputMapping);

        try {
            RUtils.loadGenericInputs(genPortMapping, connection);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to convert generic node inputs into r workspace variables: " + e);
        }

        // second, push the table inputs
        Map<String, BufferedDataTable> tablePortMapping = getDataTablePorts(inputMapping);

        try {
            RUtils.loadTableInputs(connection, tablePortMapping, exec);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to convert table node inputs into r workspace variables: " + e);
        }

        return inputMapping;
    }


    private static void loadTableInputs(RConnection connection, Map<String, BufferedDataTable> tableMapping, ExecutionContext exec) throws REXPMismatchException, RserveException, CanceledExecutionException {
        Map<String, REXP> pushTable = new HashMap<String, REXP>();

        for (String varName : tableMapping.keySet()) {
            BufferedDataTable input = tableMapping.get(varName);

            if (input == null) {
                throw new RuntimeException("null tables are not allowed in the table input mapping");
            }

            RList inputAsRList = convert2RList(exec, input);

            pushTable.put(varName, REXP.createDataFrame(inputAsRList));
        }

        RUtils.push2R(connection, pushTable);
    }


    public static Map<String, REXP> push2R(RConnection connection, Map<String, REXP> pushTable) throws RserveException, REXPMismatchException {
        for (String parName : pushTable.keySet()) {
            REXP dataFrame = pushTable.get(parName);
            connection.assign(parName, dataFrame);

            // post-process nominal attributes to convert missing values to actual NA in R
            RList rList = dataFrame.asList();

            for (Object columnKey : rList.keys()) {
                REXPVector column = (REXPVector) rList.get(columnKey);
                if (column.isString()) {
                    String varName = columnKey.toString();
                    String rVarName = parName + "$\"" + varName + "\"";

                    connection.eval(rVarName + " = ifelse(" + rVarName + " == \"NA\", NA," + rVarName + ")");
                }
            }
        }

        return pushTable;
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
     */
    public static Map<String, Object> createPortMapping(PortObject[] inObjects) {

        Map<String, Object> portVarMapping = new TreeMap<String, Object>();


        int numConnectedInputs = 0;
        for (PortObject inport : inObjects) {
            if (inport != null) {
                numConnectedInputs++;
            }
        }

        List<String> parNames = new ArrayList<String>();

        for (int i = 0; i < inObjects.length; i++) {
            parNames.add(RSnippetNodeModel.R_INVAR_BASE_NAME + (numConnectedInputs > 1 ? (i + 1) : ""));
        }

        for (int i = 0; i < inObjects.length; i++) {
            PortObject inport = inObjects[i];

            if (inport == null) {
                continue;
            }

            if (inport instanceof RPortObject) {
                portVarMapping.put(parNames.get(i), ((RPortObject) inport).getFile());
            } else if (inport instanceof BufferedDataTable) {
                portVarMapping.put(parNames.get(i), inport);
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
}
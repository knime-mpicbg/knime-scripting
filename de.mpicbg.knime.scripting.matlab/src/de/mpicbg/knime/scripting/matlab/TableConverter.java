package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.knutils.Attribute;
import de.mpicbg.knime.knutils.AttributeUtils;
import de.mpicbg.knime.knutils.InputTableAttribute;
//import de.mpicbg.math.toolintegration.matlab.MatlabWebClient;
import de.mpicbg.knime.scripting.matlab.srv.MatlabWebClient;
import org.knime.core.data.*;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

import java.io.*;
import java.util.*;


/**
 * A colleciton of static utlity method that allow to convert RM-data-strucutures to Matlab and back.
 *
 * @author Holger Brandl
 *         <p/>
 *         TODO - include the option to select different MATLAB types.
 */
@Deprecated
public class TableConverter {


    public static List<String> generateVariableNames(MatlabWebClient matlab, List<String> columnNames) {
        List<String> simpleChars = new ArrayList<String>();
        for (String columnName : columnNames) {
            columnName = columnName.replaceAll("[^0-9a-zA-Z_]", "");
            simpleChars.add(columnName);
        }
        matlab.setStringList("tmp", simpleChars);
        matlab.eval("tmp = genvarname(tmp);");
        String[] variableNames = matlab.getStringList("tmp");
        matlab.clearWorkspace("tmp");
        return Arrays.asList(variableNames);
    }


    public static List<String> getColumnNames(DataTableSpec tableSpec) {
        List<String> colNames = new ArrayList<String>();
        for (DataColumnSpec colSpec : tableSpec) {
            colNames.add(colSpec.getName());
        }
        return colNames;
    }


    public static void pushData2Matlab(MatlabWebClient matlab, BufferedDataTable inputTable, String varName) {
        int numRows = inputTable.getRowCount();
        List<String> columnNames = getColumnNames(inputTable.getDataTableSpec());
        List<String> matlabColumnNames = generateVariableNames(matlab, columnNames);

        // initialize the "dataset" in MATLAB
        matlab.eval(varName + " = dataset();");

        List<DataRow> tableRows = new ArrayList<DataRow>();
        for (DataRow dataRow : inputTable) {
            tableRows.add(dataRow);
        }

        // add all its values
        int colCounter = 0;
        for (DataColumnSpec columnSpec : inputTable.getDataTableSpec()) {

            DataType colType = columnSpec.getType();
            int counter;
            if (colType.equals(StringCell.TYPE)) {
                List<String> strColumn = new ArrayList<String>();
                counter = 0;
                for (DataRow dataRow : tableRows) {
                    if (dataRow.getCell(colCounter).isMissing()) {
                        strColumn.add("");
                    } else {
                        strColumn.add(((StringCell) dataRow.getCell(colCounter)).getStringValue());
                    }
                }
                matlab.setStringList("vector", strColumn);

            } else if (colType.equals(DoubleCell.TYPE)) {
                double[] numColumn = new double[numRows];
                counter = 0;
                for (DataRow dataRow : tableRows) {
                    if (dataRow.getCell(colCounter).isMissing()) {
                        numColumn[counter++] = Double.NaN;
                    } else {
                        numColumn[counter++] = ((DoubleCell) dataRow.getCell(colCounter)).getDoubleValue();
                    }
                }
                matlab.setVector("vector", numColumn);

            } else if (colType.equals(IntCell.TYPE)) {
                double[] numColumn = new double[numRows];
                counter = 0;
                for (DataRow dataRow : tableRows) {
                    if (dataRow.getCell(colCounter).isMissing()) {
                        numColumn[counter++] = Double.NaN;
                    } else {
                        numColumn[counter++] = ((IntCell) dataRow.getCell(colCounter)).getDoubleValue();
                    }
                }
                matlab.setVector("vector", numColumn);
            }

            //Parse the column in the "dataset".
            matlab.eval(varName + " = cat(2, " + varName + ", dataset({vector(:), '" + matlabColumnNames.get(colCounter) + "'}));");
            colCounter++;
        }

        // Set the variable descriptions (column names) and the observation index.
        matlab.setStringList("varDes", columnNames);
        matlab.eval("index=1:length(vector);index=cellstr(num2str(index(:)));" + varName + "=set(" + varName + ",'ObsNames',index,'VarDescription',varDes);");
        matlab.clearWorkspace("vector", "index", "varDes");
    }


    public static BufferedDataTable pullDataFromMatlab(ExecutionContext exec, MatlabWebClient matlab, String varName) {
        // Get the column names.
        String[] attributeNames = matlab.getStringList("get(" + varName + ", 'VarNames');");
        String[] attributeDescriptions = matlab.getStringList("get(" + varName + ", 'VarDescription');");
        // Take the variable name where no description is available.
        if (attributeDescriptions.length == 0) {
            attributeDescriptions = attributeNames;
        } else {
            for (int i = 0; i < attributeDescriptions.length; i++) {
                String attributeDescription = attributeDescriptions[i];
                if (attributeDescription.isEmpty()) {
                    attributeDescriptions[i] = attributeNames[i];
                }
            }
        }

        // Initialize the KNIME table
        DataColumnSpec[] colSpecs = new DataColumnSpec[attributeNames.length];
        Map<DataColumnSpec, Object> columnData = new HashMap<DataColumnSpec, Object>();

        //1)  create attributes
        int numExamples = -1;
        for (int attrCounter = 0; attrCounter < attributeNames.length; attrCounter++) {

            String attributeName = attributeNames[attrCounter];
            String attributeDescription = attributeDescriptions[attrCounter];
            String columnAccessString = varName + "." + attributeName;

            if (matlab.getScalar("+isfloat(" + columnAccessString + ");") == 1) {

                colSpecs[attrCounter] = new DataColumnSpecCreator(attributeDescription, DoubleCell.TYPE).createSpec();
                double[] numColumnData = matlab.getVector(columnAccessString + ";");
                columnData.put(colSpecs[attrCounter], numColumnData);
                numExamples = numColumnData.length;

            } else if (matlab.getScalar("+any(cellfun(@ischar," + columnAccessString + "));") == 1) {

                colSpecs[attrCounter] = new DataColumnSpecCreator(attributeDescription, StringCell.TYPE).createSpec();
                String[] strColumnData = matlab.getStringList(columnAccessString + ";");
                columnData.put(colSpecs[attrCounter], strColumnData);
                numExamples = strColumnData.length;

            } else if (matlab.getScalar("+islogical(" + columnAccessString + ");") == 1) {
//                attribute = AttributeFactory.createAttribute(columnName, Ontology.BINOMINAL);
                throw new RuntimeException("logical columns are not yet supported");

            } else {
                throw new RuntimeException("column type not supported");
            }
        }

        //2) create data rows
        assert numExamples > 0;
        DataCell[][] cells = new DataCell[numExamples][colSpecs.length];

        for (int colIndex = 0; colIndex < colSpecs.length; colIndex++) {
            DataColumnSpec colSpec = colSpecs[colIndex];
            Object curColumn = columnData.get(colSpec);

            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                double[] doubleColumn = (double[]) curColumn;

                for (int rowIndex = 0; rowIndex < numExamples; rowIndex++) {
                    cells[rowIndex][colIndex] = new DoubleCell(doubleColumn[rowIndex]);
                }

            } else if (colSpec.getType().isCompatible(StringValue.class)) {
                String[] stringColumn = (String[]) curColumn;

                for (int rowIndex = 0; rowIndex < numExamples; rowIndex++) {
                    if (stringColumn[rowIndex] == null) {
                        cells[rowIndex][colIndex] = DataType.getMissingCell();
                    } else {
                        if (stringColumn[rowIndex].isEmpty()) {
                            cells[rowIndex][colIndex] = DataType.getMissingCell();
                        } else {
                            cells[rowIndex][colIndex] = new StringCell(stringColumn[rowIndex]);
                        }
                    }
                }
            }
            // todo maybe we have to treat other types here
//            exec.checkCanceled();
            exec.setProgress(colIndex / (double) colSpecs.length, "Adding row " + colIndex);
        }


        DataTableSpec outputSpec = new DataTableSpec(colSpecs);
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        for (int i = 0; i < numExamples; i++) {
            RowKey key = new RowKey("" + i);
            DataRow row = new DefaultRow(key, cells[i]);
            container.addRowToTable(row);
        }

        // once we are done, we close the container and return its table
        container.close();
        return container.getTable();
    }


    //
    // Set of alternative methods to organize the data transfer via serialized HashMaps.
    //

    public static LinkedHashMap convertKnimeTableToLinkedHashMap(BufferedDataTable table) {
        DataTableSpec tableSpec = table.getDataTableSpec();
        // Initialize the hash.
        LinkedHashMap hashTable = new LinkedHashMap();
        for (int j = 0; j < tableSpec.getNumColumns(); j++) {

            DataColumnSpec columnSpec = tableSpec.getColumnSpec(j);
            if (columnSpec.getType().isCompatible(StringValue.class)) {
                hashTable.put(columnSpec.getName(), new String[table.getRowCount()]);
            } else {
                hashTable.put(columnSpec.getName(), new Double[table.getRowCount()]);
            }
        }
        // Parse the data from the KNIME table into the hash.
        int i = 0;
        for (DataRow row : table) {

            for (int j = 0; j < row.getNumCells(); j++) {

                DataColumnSpec columnSpec = tableSpec.getColumnSpec(j);

                if (columnSpec.getType().isCompatible(StringValue.class)) {
                    String[] str = (String[]) hashTable.get(columnSpec.getName());
                    Attribute readoutAttribute = new InputTableAttribute(columnSpec.getName(), table);
                    str[i] = readoutAttribute.getNominalAttribute(row);
                    hashTable.put(columnSpec.getName(), str);
                } else {
                    Double[] num = (Double[]) hashTable.get(columnSpec.getName());
                    Attribute readoutAttribute = new InputTableAttribute(columnSpec.getName(), table);
                    num[i] = readoutAttribute.getDoubleAttribute(row);
                    hashTable.put(columnSpec.getName(), num);
                }
            }
            i++;
        }
        return hashTable;
    }


    public static List<Attribute> createColumnSpecifications(LinkedHashMap hash) {
        // Initialize the columnSpec table
        List<Attribute> colSpec = new ArrayList<Attribute>();
        Attribute attribute;
        for (Object attributeKey : hash.keySet()) {
            Object colData = hash.get(attributeKey);
            if (colData instanceof int[]) {
                attribute = new Attribute(attributeKey.toString(), IntCell.TYPE);
            } else if (colData instanceof double[]) {
                attribute = new Attribute(attributeKey.toString(), DoubleCell.TYPE);
            } else if ((colData instanceof List) || (colData instanceof String[])) {
                attribute = new Attribute(attributeKey.toString(), StringCell.TYPE);
            } else if (colData instanceof boolean[]) {
                throw new RuntimeException("logical columns are not yet supported");
            } else {
                // If the proper way does not work, try it dirty.
                try {
                    colData = (double[]) colData;
                    attribute = new Attribute(attributeKey.toString(), DoubleCell.TYPE);
                } catch (Exception e) {
                    try {
                        colData = (int[]) colData;
                        attribute = new Attribute(attributeKey.toString(), IntCell.TYPE);
                    } catch (Exception ee) {
                        try {
                            colData = (String[]) colData;
                            attribute = new Attribute(attributeKey.toString(), StringCell.TYPE);
                        } catch (Exception eee) {
                            System.err.println("Unsupported column type: " + colData.getClass().getName());
                            continue;
                        }
                    }
                }
            }
            colSpec.add(attribute);
        }
        return colSpec;
    }


    public static BufferedDataTable convertLinkedHashMapToKnimeTable(ExecutionContext exec, LinkedHashMap dataFromMatlab) {
        List<Attribute> colAttributes = createColumnSpecifications(dataFromMatlab);
        BufferedDataTable table = convertLinkedHashMapToKnimeTable(exec, dataFromMatlab, colAttributes);
        return table;
    }


    public static BufferedDataTable convertLinkedHashMapToKnimeTable(ExecutionContext exec, LinkedHashMap dataFromMatlab, List<Attribute> colAttributes) {
        // Get the number of samples (rows)
        int numSamples = -1;
        Attribute firstAttribute = colAttributes.get(0);
        Object colData = dataFromMatlab.get(firstAttribute.getName());
        if (firstAttribute.getType().isCompatible(DoubleValue.class)) {
            numSamples = ((double[]) colData).length;
        } else if ((firstAttribute.getType().isCompatible(IntValue.class))) {
            numSamples = ((int[]) colData).length;
        } else if (firstAttribute.getType().isCompatible(StringValue.class)) {
            numSamples = ((String[]) colData).length;
        }

        // create the cell matrix
        assert numSamples > 0;
        int columnLength = numSamples;
        DataCell[][] cells = new DataCell[numSamples][colAttributes.size()];

        for (int colIndex = 0; colIndex < colAttributes.size(); colIndex++) {
            Attribute columnAttribute = colAttributes.get(colIndex);
            Object curColumn = dataFromMatlab.get(columnAttribute.getName());

            if (columnAttribute.getType().isCompatible(DoubleValue.class)) {
                double[] doubleColumn = (double[]) curColumn;
                columnLength = doubleColumn.length;
                for (int rowIndex = 0; rowIndex < numSamples; rowIndex++) {
                    cells[rowIndex][colIndex] = new DoubleCell(doubleColumn[rowIndex]);
                }
            } else if (columnAttribute.getType().isCompatible(IntValue.class)) {
                int[] intColumn = (int[]) curColumn;
                columnLength = intColumn.length;
                for (int rowIndex = 0; rowIndex < numSamples; rowIndex++) {
                    cells[rowIndex][colIndex] = new IntCell(intColumn[rowIndex]);
                }
            } else if (columnAttribute.getType().isCompatible(StringValue.class)) {
                String[] stringColumn = (String[]) curColumn;
                columnLength = stringColumn.length;
                for (int rowIndex = 0; rowIndex < numSamples; rowIndex++) {
                    if (stringColumn[rowIndex] == null) {
                        cells[rowIndex][colIndex] = DataType.getMissingCell();
                    } else {
                        if (stringColumn[rowIndex].isEmpty()) {
                            cells[rowIndex][colIndex] = DataType.getMissingCell();
                        } else {
                            cells[rowIndex][colIndex] = new StringCell(stringColumn[rowIndex]);
                        }
                    }
                }
            }
            if (columnLength != numSamples) {
                throw new RuntimeException("The Columns do not have the same lenght!");
            }
        }

        // convert cell matrix into knime table
        DataTableSpec outputSpec = AttributeUtils.compileTableSpecs(colAttributes);
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        for (int i = 0; i < numSamples; i++) {
            RowKey key = new RowKey("" + i);
            DataRow row = new DefaultRow(key, cells[i]);
            container.addRowToTable(row);
        }

        container.close();
        return container.getTable();
    }


    public static void writeHashMapToTempFolder(String filePath, LinkedHashMap hash) throws Exception {
        File file = new File(filePath);
        file.deleteOnExit();
        FileOutputStream fileStream = new FileOutputStream(file);
        ObjectOutputStream serializedObject = new ObjectOutputStream(fileStream);
        serializedObject.writeObject(hash);
        serializedObject.close();
    }


    public static LinkedHashMap readSerializedHashMap(File file) throws Exception {
        InputStream fileStream = new FileInputStream(file);
        ObjectInputStream objectStream = new ObjectInputStream(fileStream);
        return (LinkedHashMap) objectStream.readObject();
    }


}
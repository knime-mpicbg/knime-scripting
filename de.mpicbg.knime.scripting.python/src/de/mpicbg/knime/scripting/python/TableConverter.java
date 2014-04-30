package de.mpicbg.knime.scripting.python;

/**
 * A colleciton of static utlity method that allow to convert RM-data-strucutures to Matlab and back.
 *
 * @author Holger Brandl
 *         <p/>
 *         TODO
 *         - include the option to select different MATLAB types.
 */

public class TableConverter {

//    public static List<String> generateVariableNames(MatlabClient matlab, List<String> columnNames) {
//        List<String> simpleChars = new ArrayList<String>();
//        for (String columnName : columnNames) {
//            columnName = columnName.replaceAll("[^0-9a-zA-Z_]", "");
//            simpleChars.add(columnName);
//        }
//        matlab.setStringList("tmp", simpleChars);
//        matlab.eval("tmp = genvarname(tmp);");
//        String[] variableNames = matlab.getStringList("tmp");
//        matlab.clear("tmp");
//        return Arrays.asList(variableNames);
//    }
//
//
//    public static List<String> getColumnNames(DataTableSpec tableSpec) {
//        List<String> colNames = new ArrayList<String>();
//        for (DataColumnSpec colSpec : tableSpec) {
//            colNames.add(colSpec.getName());
//        }
//        return colNames;
//    }
//
//
//    public static void push2Matlab(MatlabClient matlab, BufferedDataTable inputTable) {
//
//        int numRows = inputTable.getRowCount();
//        List<String> columnNames = getColumnNames(inputTable.getDataTableSpec());
//        List<String> matlabColumnNames = generateVariableNames(matlab, columnNames);
//
//        // initialize the "dataset" in MATLAB
//        matlab.eval("inA = dataset();");
//
//        // add all its values
//        int colCounter = 0;
//        for (DataColumnSpec columnSpec : inputTable.getDataTableSpec()) {
//
//            DataType colType = columnSpec.getType();
//            int counter;
//            if (colType.equals(StringCell.TYPE)) {
//                List<String> strColumn = new ArrayList<String>();
//
//                counter = 0;
//                for (DataRow dataRow : inputTable) {
//                    if (dataRow.getCell(colCounter).isMissing()) {
//                        strColumn.add("");
//                    } else {
//                        strColumn.add(((StringCell) dataRow.getCell(colCounter)).getStringValue());
//                    }
//                }
//
//                matlab.setStringList("vector", strColumn);
//
//            } else if (colType.equals(DoubleCell.TYPE)) {
//                double[] numColumn = new double[numRows];
//
//                counter = 0;
//                for (DataRow dataRow : inputTable) {
//                    numColumn[counter++] = ((DoubleCell) dataRow.getCell(colCounter)).getDoubleValue();
//                }
//
//                matlab.setVector("vector", numColumn);
//            } else if (colType.equals(IntCell.TYPE)) {
//                double[] numColumn = new double[numRows];
//
//                counter = 0;
//                for (DataRow dataRow : inputTable) {
//                    numColumn[counter++] = ((IntCell) dataRow.getCell(colCounter)).getDoubleValue();
//                }
//
//                matlab.setVector("vector", numColumn);
//            }
//
//            //Parse the column in the "dataset".
//            matlab.eval("inA = cat(2, inA, dataset({vector(:), '" + matlabColumnNames.get(colCounter) + "'}));");
//            colCounter++;
//        }
//
//        // Set the variable descriptions (column names) and the observation index.
//        matlab.setStringList("varDes", columnNames);
//        matlab.eval("index=1:length(vector);index=cellstr(num2str(index(:)));inA=set(inA,'ObsNames',index,'VarDescription',varDes);");
//        matlab.clear("vector", "index", "varDes");
//    }
//
//
//    public static BufferedDataTable readDataFrameFromMatlab(ExecutionContext exec, MatlabClient matlab, String varName) {
//
//        // Get the column names.
//        String[] attributeNames = matlab.getStringList("get(inA, 'VarNames');");
//        String[] attributeDescriptions = matlab.getStringList("get(inA, 'VarDescription');");
//        if (attributeDescriptions.length == 0) {
//            attributeDescriptions = attributeNames;
//        }
//
//        // Initialize the KNIME table
//        DataColumnSpec[] colSpecs = new DataColumnSpec[attributeNames.length];
//        Map<DataColumnSpec, Object> columnData = new HashMap<DataColumnSpec, Object>();
//
//        //1)  create attributes
//        int numExamples = -1;
//        for (int attrCounter = 0; attrCounter < attributeNames.length; attrCounter++) {
//
//            String attributeName = attributeNames[attrCounter];
//            String attributeDescription = attributeDescriptions[attrCounter];
//            String columnAccessString = varName + "." + attributeName;
//
//            if (matlab.getScalar("+isfloat(" + columnAccessString + ");") == 1) {
//
//                colSpecs[attrCounter] = new DataColumnSpecCreator(attributeDescription, DoubleCell.TYPE).createSpec();
//                double[] numColumnData = matlab.getVector(columnAccessString + ";");
//                columnData.put(colSpecs[attrCounter], numColumnData);
//                numExamples = numColumnData.length;
//
//            } else if (matlab.getScalar("+any(cellfun(@ischar," + columnAccessString + "));") == 1) {
//
//                colSpecs[attrCounter] = new DataColumnSpecCreator(attributeDescription, StringCell.TYPE).createSpec();
//                String[] strColumnData = matlab.getStringList(columnAccessString + ";");
//                columnData.put(colSpecs[attrCounter], strColumnData);
//                numExamples = strColumnData.length;
//
//            } else if (matlab.getScalar("+islogical(" + columnAccessString + ");") == 1) {
////                attribute = AttributeFactory.createAttribute(columnName, Ontology.BINOMINAL);
//                throw new RuntimeException("logical columns are not yet supported");
//
//            } else {
//                throw new RuntimeException("column type not supported");
//            }
//        }
//
//        //2) create data rows
//        DataTableSpec outputSpec = new DataTableSpec(colSpecs);
//        BufferedDataContainer container = exec.createDataContainer(outputSpec);
//
//        assert numExamples > 0;
//        DataCell[][] cells = new DataCell[numExamples][colSpecs.length];
//
//        for (int colIndex = 0; colIndex < colSpecs.length; colIndex++) {
//            DataColumnSpec colSpec = colSpecs[colIndex];
//            Object curColumn = columnData.get(colSpec);
//
//            if (colSpec.getType().equals(DoubleCell.TYPE)) {
//                double[] doubleColumn = (double[]) curColumn;
//
//                for (int rowIndex = 0; rowIndex < numExamples; rowIndex++) {
//                    cells[rowIndex][colIndex] = new DoubleCell(doubleColumn[rowIndex]);
//                }
//
//            } else if (colSpec.getType().equals(StringCell.TYPE)) {
//                String[] stringColumn = (String[]) curColumn;
//
//                for (int rowIndex = 0; rowIndex < numExamples; rowIndex++) {
//                    if (stringColumn[rowIndex] == null) {
//                        cells[rowIndex][colIndex] = DataType.getMissingCell();
//                    } else {
//                        if (stringColumn[rowIndex].isEmpty()) {
//                            cells[rowIndex][colIndex] = DataType.getMissingCell();
//                        } else {
//                            cells[rowIndex][colIndex] = new StringCell(stringColumn[rowIndex]);
//                        }
//                    }
//                }
//            }
//            // todo maybe we have to treat other types here
//        }
//
//        for (int i = 0; i < numExamples; i++) {
//            RowKey key = new RowKey("" + i);
//            DataRow row = new DefaultRow(key, cells[i]);
//            container.addRowToTable(row);
//        }
//
////            exec.checkCanceled();
////            exec.setProgress(i / (double)m_count.getIntValue(), "Adding row " + i);
////        // once we are done, we close the container and return its table
//
//        container.close();
//
//        return container.getTable();
//    }
}
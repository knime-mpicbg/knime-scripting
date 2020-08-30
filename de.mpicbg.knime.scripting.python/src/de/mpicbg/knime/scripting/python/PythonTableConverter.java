package de.mpicbg.knime.scripting.python;


import de.mpicbg.knime.knutils.InputTableAttribute;

import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVParser;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser to convert KNIME table to something the 
 * Python interpreter can work with
 * 
 * note: this class works with Python nodes version 1
 * 
 */
public class PythonTableConverter {
	
	/**
	 * read CSV and return KNIME table
	 * 
	 * @param exec
	 * @param pyOutFile		- CSV from Python
	 * @param logger
	 * @return	KNIME table
	 * 
	 * @throws RuntimeException
	 */
    public static BufferedDataTable convertCSVToTable(ExecutionContext exec, File pyOutFile, NodeLogger logger) throws RuntimeException {
        try {
        	// csv reader needs to read quotes to get the difference of "nan" versus nan (python representation of missing values)
        	final CSVParser parser =
        			new CSVParserBuilder().withQuoteChar('\0').withSeparator(',').build();
        	final CSVReader reader =
        			new CSVReaderBuilder(new BufferedReader(new FileReader(pyOutFile)))
        			.withCSVParser(parser)
        			.build();
        	
        	// csv reader needs to read quotes to get the difference of "nan" versus nan (python representation of missing values)
            //CSVReader reader = new CSVReader(new BufferedReader(new FileReader(pyOutFile)), ',', '\0');

            String[] columnNames = reader.readNext();
            String[] columnTypes = reader.readNext();
            
            assert(columnNames.length == columnTypes.length);
            
            // remove quote character from beginning and end
            char quoteChar = columnNames[0].charAt(0);
            for(int i = 0; i < columnNames.length; i++) {
            	columnNames[i] = removeQuotes(columnNames[i]);
            	columnTypes[i] = removeQuotes(columnTypes[i]);
            }

            // Create data rows
            DataColumnSpec[] colSpecs = new DataColumnSpec[columnTypes.length];

            // Loop over the column types and create the columns
            for (int i = 0; i < columnTypes.length; i++) {
                if ("INT".equals(columnTypes[i]))
                    colSpecs[i] = new DataColumnSpecCreator(columnNames[i], IntCell.TYPE).createSpec();
                else if ("FLOAT".equals(columnTypes[i]))
                    colSpecs[i] = new DataColumnSpecCreator(columnNames[i], DoubleCell.TYPE).createSpec();
                else colSpecs[i] = new DataColumnSpecCreator(columnNames[i], StringCell.TYPE).createSpec();
            }

            DataTableSpec tableSpec = new DataTableSpec(colSpecs);
            BufferedDataContainer container = exec.createDataContainer(tableSpec);

            String[] line;
            int rowNum = 0;
            while ((line = reader.readNext()) != null) {
                DataCell[] cells = new DataCell[columnNames.length];

                for (int i = 0; i < columnTypes.length; i++) {
                    DataColumnSpec colSpec = colSpecs[i];
                    String value = line[i];
                    
                    //if the value starts with ", it's a string
                    if(value.charAt(0) == quoteChar) {
                    	cells[i] = new StringCell(removeQuotes(value));
                    } else {
                    	// Create the cell based on the column type
                        try {
                            if (colSpec.getType().isCompatible(IntValue.class)) {
                                cells[i] = new IntCell(Integer.parseInt(value));
                            } else if (colSpec.getType().isCompatible(DoubleValue.class))
                                cells[i] = new DoubleCell(Double.parseDouble(value));
                            else {
                            	// it should be a string but did not start with quotechar, so it's missing?
                            	if(value.equals("nan"))
                            		cells[i] = DataType.getMissingCell();
                            	else throw new RuntimeException("unsupported string found in CSV:" + value + "\n" + pyOutFile.getAbsolutePath());
                            }
                        } catch (NumberFormatException e) {
                            cells[i] = DataType.getMissingCell();
                        }
                    }
                }

                RowKey key = new RowKey("" + rowNum++);
                DataRow row = new DefaultRow(key, cells);
                container.addRowToTable(row);
            }

            reader.close();
            container.close();

            return container.getTable();
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException(e);
        } 
    }

    /**
     * remove first and last character of a string
     * 
     * @param string
     * @return
     */
    private static String removeQuotes(String string) {
    	if(string == null) return null;
    	if(string.length() < 2) return null;
    	return string.substring(1, string.length()-1);
	}

    /**
     * from KNIME tablespec retrieve list of column names
     * 
     * @param tableSpec
     * @return
     */
	private static List<String> getColumnNames(DataTableSpec tableSpec) {
        List<String> colNames = new ArrayList<String>();
        for (DataColumnSpec colSpec : tableSpec) {
            colNames.add(colSpec.getName());
        }
        return colNames;
    }

	/**
	 * from KNIME tablespec retrieve list of column data types
	 * 
	 * @param tableSpec
	 * @return
	 */
    private static List<DataType> getColumnTypes(DataTableSpec tableSpec) {
        List<DataType> colTypes = new ArrayList<DataType>();
        for (DataColumnSpec colSpec : tableSpec) {
            colTypes.add(colSpec.getType());
        }

        return colTypes;
    }

    /**
     * Write a table to a CSV file.  The first line of the file is a CSV array of column names, the second is
     * a CSV array of column types, all subsequent lines are CSV rows of the table.
     *
     * @param exec
     * @param inputTable
     * @param kInFile
     * @throws RuntimeException
     */
    public static void convertTableToCSV(ExecutionContext exec, BufferedDataTable inputTable, File kInFile, NodeLogger logger) throws RuntimeException {
        try {
            //CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(kInFile)), ',', '\"');
        	CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(kInFile)), ',', '\"', ICSVParser.DEFAULT_ESCAPE_CHARACTER, ICSVWriter.DEFAULT_LINE_END);
            DataTableSpec tableSpec = inputTable.getDataTableSpec();

            List<String> columnNames = getColumnNames(tableSpec);

            // The first line is the column names
            writer.writeNext(columnNames.toArray(new String[0]));

            // Build a list of column types
            List<DataType> colTypes = getColumnTypes(tableSpec);
            ArrayList<String> outputTypes = new ArrayList<String>();

            for (DataType type : colTypes) {
                if (type.equals(IntCell.TYPE)) outputTypes.add("INT");
                else if (type.equals(DoubleCell.TYPE)) outputTypes.add("FLOAT");
                else if (type.equals(StringCell.TYPE)) outputTypes.add("STRING");
                else if (type.isCompatible(StringValue.class)) outputTypes.add("STRING");
            }

            // The second line is the column types
            writer.writeNext(outputTypes.toArray(new String[0]));

            // Add all the table values, row by row
            ArrayList<String> rowValues = new ArrayList<String>();

            for (DataRow dataRow : inputTable) {
                int colNum = 0;
                for (DataColumnSpec columnSpec : tableSpec) {
                    DataType colType = colTypes.get(colNum);
                    DataCell cell = dataRow.getCell(colNum++);

                    InputTableAttribute a = new InputTableAttribute(columnSpec.getName(), inputTable);
                    //Attribute a = new Attribute(columnSpec.getName(), columnSpec.getType());
                    if (cell.isMissing()) rowValues.add("");
                    else if (colType.equals(StringCell.TYPE)) {
                        rowValues.add(a.getNominalAttribute(dataRow));
                    } else if (colType.equals(DoubleCell.TYPE)) {
                        rowValues.add(a.getDoubleAttribute(dataRow).toString());
                    } else if (colType.equals(IntCell.TYPE)) {
                        rowValues.add(a.getIntAttribute(dataRow).toString());
                    } else {
                    	rowValues.add(a.getRawValue(dataRow));
                    }                    
                }

                // All the columns have been added so write it out
                writer.writeNext(rowValues.toArray(new String[0]));
                rowValues.clear();
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

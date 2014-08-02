package de.mpicbg.knime.scripting.matlab.srv.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

import de.mpicbg.knime.knutils.Attribute;
import de.mpicbg.knime.knutils.AttributeUtils;
import de.mpicbg.knime.knutils.InputTableAttribute;
import de.mpicbg.knime.scripting.matlab.srv.Matlab;


/**
 * This Class is a wrapper for KNIME tables to provide MATLAB 
 * compatibility for the data.
 * 
 * @author Holger Brandl, Felix Meyenhofer
 */
public class MatlabTable {
	
	/** KNIME table object */
	private BufferedDataTable table;
	
	/** KNIME table in the for of a linked hash-map */
	private LinkedHashMap<String, Object> hash;
	
	/** Temp file for the data */
	private File hashTempFile;
	
	
	/**
	 * Constructor 
	 * 
	 * @param table KNIME table
	 */
	public MatlabTable(BufferedDataTable table) {
		this.table = table;
	}
	
	/**
	 * Constructor
	 * 
	 * @param hash KNIME data in the form of a linked hash-map.
	 */
	public MatlabTable(LinkedHashMap<String, Object> hash) {
		this.hash = hash;
	}
	
	
	/**
	 * Getter for the temp-file containing the KNIME table data
	 * 
	 * @return {@link File} pointing to the hash-map object dump
	 */
	public File getTempFile() {
		return this.hashTempFile;
	}
	
	/**
	 * Getter for the KNIME table
	 * 
	 * @return KNIME table
	 */
	public BufferedDataTable getBufferedDataTable() {
		return this.table;
	}
	
	/**
	 * Getter for the transformed KNIME table
	 * 
	 * @return Transformed KNIME table
	 */
	public LinkedHashMap<String, Object> getLinkedHashMap() {
		return this.hash;
	}
	
	/**
     * Conversion of a KNIME table (see {@link BufferedDataTable}) to a {@link LinkedHashMap}
     * 
     * @param table Input table form KNIME
     * @return {@link LinkedHashMap}
     */
    public void knimeTable2LinkedHashMap() {
        DataTableSpec tableSpec = this.table.getDataTableSpec();
        
        // Initialize the hash.
        LinkedHashMap<String, Object> hashTable = new LinkedHashMap<String, Object>();
        for (int j = 0; j < tableSpec.getNumColumns(); j++) {
            DataColumnSpec columnSpec = tableSpec.getColumnSpec(j);
            if (columnSpec.getType().isCompatible(StringValue.class)) {
                hashTable.put(columnSpec.getName(), new String[this.table.getRowCount()]);
            } else {
                hashTable.put(columnSpec.getName(), new Double[this.table.getRowCount()]);
            }
        }
        
        // Parse the data from the KNIME table into the hash.
        int i = 0;
        for (DataRow row : this.table) {

            for (int j = 0; j < row.getNumCells(); j++) {

                DataColumnSpec columnSpec = tableSpec.getColumnSpec(j);

                if (columnSpec.getType().isCompatible(StringValue.class)) {
                    String[] str = (String[]) hashTable.get(columnSpec.getName());
                    Attribute<StringValue> readoutAttribute = new InputTableAttribute<StringValue>(columnSpec.getName(), this.table);
                    str[i] = readoutAttribute.getNominalAttribute(row);
                    hashTable.put(columnSpec.getName(), str);
                } else {
                    Double[] num = (Double[]) hashTable.get(columnSpec.getName());
                    Attribute<DoubleValue> readoutAttribute = new InputTableAttribute<DoubleValue>(columnSpec.getName(), this.table);
                    num[i] = readoutAttribute.getDoubleAttribute(row);
                    hashTable.put(columnSpec.getName(), num);
                }
            }
            i++;
        }
        
        this.hash = hashTable;
    }
	
	/**
	 * Transform the {@link LinkedHashMap} back to a {@link BufferedDataTable} 
	 * (MATLAB java object back to KNIME data table)
	 *  
	 * @param exec Execution context of the KNIME node
	 */
    public void linkedHashMap2KnimeTable(ExecutionContext exec) {
    	
    	// Determine the column attributes
    	List<Attribute> colAttributes = createColumnSpecifications(this.hash);
    	
        // Get the number of samples (rows)
        int numSamples = -1;
        Attribute firstAttribute = colAttributes.get(0);
        Object colData = this.hash.get(firstAttribute.getName());
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
            Object curColumn = this.hash.get(columnAttribute.getName());

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

        // convert cell matrix into KNIME table
        DataTableSpec outputSpec = AttributeUtils.compileTableSpecs(colAttributes);
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        for (int i = 0; i < numSamples; i++) {
            RowKey key = new RowKey("" + i);
            DataRow row = new DefaultRow(key, cells[i]);
            container.addRowToTable(row);
        }

        container.close();
        this.table = container.getTable();
    }
    
	/**
	 * Make an object dump of the KNIME data in the MATLAB understandable
	 * java object.
	 * 
	 * @throws IOException
	 */
    public void writeHashMapToTempFolder() throws IOException {
    	if (this.hash == null)
    		knimeTable2LinkedHashMap();
    	
        File file = File.createTempFile(Matlab.TABLE_TEMP_FILE_PREFIX, Matlab.TABLE_TEMP_FILE_SUFFIX);
        file.deleteOnExit();
        FileOutputStream fileStream = new FileOutputStream(file);
        ObjectOutputStream serializedObject = new ObjectOutputStream(fileStream);
        serializedObject.writeObject(this.hash);
        serializedObject.close();
        this.hashTempFile = file;
        this.hash = null;
    }
    
    /**
     * Read the MATLAB understandable java object dump.
     * 
     * @param exec Node execution context
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void readHashMapFromTempFolder(ExecutionContext exec) throws IOException, ClassNotFoundException  {
    	InputStream fileStream = new FileInputStream(this.hashTempFile);
    	ObjectInputStream objectStream = new ObjectInputStream(fileStream);
        this.hash = (LinkedHashMap<String, Object>) objectStream.readObject();;
        linkedHashMap2KnimeTable(exec);
        this.hashTempFile.delete();
    }
    
    /**
     * Create the column specs from the {@link LinkedHashMap} 
     * (MATLAB understandable java object)
     * 
     * @param hash Table data
     * @return List of KNIME table attributes (specs)
     */
    public static List<Attribute> createColumnSpecifications(LinkedHashMap<String, Object> hash) {
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

    /**
     * Cleanup the files and object to liberate disk and memory space
     * TODO: make sure we got everything.
     */
	public void cleanup() {
		if (hashTempFile != null)
			hashTempFile.delete();
		hash = null;
	}
	
}

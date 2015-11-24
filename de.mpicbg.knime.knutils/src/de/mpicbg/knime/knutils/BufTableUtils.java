package de.mpicbg.knime.knutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class BufTableUtils {


    public static void main(String[] args) {
    }


    public static void saveBinary(File dumpFile, Object object) {
        try {
            ObjectOutputStream objStream = new ObjectOutputStream(new FileOutputStream(dumpFile));
            objStream.writeObject(object);
            objStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static Object loadBinary(File dumpFile) {
        try {
            ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(dumpFile));
            return inStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    public static List<DataRow> toList(BufferedDataTable input) {
        List<DataRow> data = new ArrayList<DataRow>();

        for (DataRow dataRow : input) {
            data.add(dataRow);
        }

        return data;
    }


    /**
     * check if the execution monitor was canceled.
     */
    public static void updateProgress(ExecutionContext exec, long done, long all) throws CanceledExecutionException {
        exec.checkCanceled();
        exec.setProgress((double) done / (double) all);
    }
    
    /**
     * creates a data table spec based on a hash map with column names and types
     * @param columns
     * @return new data table spec
     */
    public static DataTableSpec createNewDataTableSpec(LinkedHashMap<String, DataType> columns) {
    	DataColumnSpecCreator colSpecCreator = null;
    	DataColumnSpec[] cSpecList = new DataColumnSpec[columns.size()];
    	int i = 0;
    	for(String columnName : columns.keySet()) {
    		colSpecCreator = new DataColumnSpecCreator(columnName, columns.get(columnName));
    		cSpecList[i] = colSpecCreator.createSpec();
    		i++;
    	}
    	
    	DataTableSpecCreator specCreator = new DataTableSpecCreator();
    	specCreator.addColumns(cSpecList);
    	
    	return specCreator.createSpec();
    }
}

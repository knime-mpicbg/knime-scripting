package de.mpicbg.knime.scripting.r.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.r.RUtils.RType;

/**
 * transfer class; from KNIME data table to R data frame
 * @author Antje Janosch
 *
 */
public class RDataFrameContainer {
	
	/**
	 * column chunk number + list of columns in that chunk
	 */
	private LinkedHashMap<Integer, List<RDataColumn>> m_columnChunks = new LinkedHashMap<Integer, List<RDataColumn>>();
	
	/**
	 * list of chunk names
	 */
	private List<String> m_chunkNames = new ArrayList<String>();
	
	/**
	 * row keys of table
	 */
	private String[] m_rowKeys;
	
	/**
	 * number of rows
	 */
	private int m_numRows;
	
	/**
	 * number of columns
	 */
	private int m_numCols;
	
	/**
	 * intermediate value for missing strings; RServe does not support NA-values for Strings
	 */
	public static final String NA_VAL_FOR_R = "NA";
	
	/**
	 * KNIME logger
	 */
	private NodeLogger logger = NodeLogger.getLogger(RDataFrameContainer.class);

	/**
	 * constructor with given table dimensions
	 * @param numRows
	 * @param numCols
	 */
	public RDataFrameContainer(int numRows, int numCols) {
		super();
		this.m_numRows = numRows;
		this.m_numCols = numCols;
		
		m_rowKeys = new String[numRows];
	}
	
	/**
	 * adds the row key of row at given index
	 * @param row
	 * @param rowKey
	 */
	public void addRowKey(int row, String rowKey) {
		m_rowKeys[row] = rowKey;
	}
	
	/**
	 * adds a new data column with a given name, data type and index and assigned to a chunk
	 * @param cName
	 * @param type
	 * @param chunk
	 * @param colIdx
	 */
	public void addColumnSpec(String cName, RType type, int chunk, int colIdx) {
		
		RDataColumn rColumn = new RDataColumn(cName, type, colIdx);
		
		if(!m_columnChunks.containsKey(chunk))
			m_columnChunks.put(chunk, new ArrayList<RDataColumn>());
		m_columnChunks.get(chunk).add(rColumn);
	}

	/**
	 * @return set of chunk indices
	 */
	public Set<Integer> getColumnChunks() {
		return m_columnChunks.keySet();
	}

	/**
	 * initialize the data vectors of all columns in a given chunk
	 * @param chunk
	 * @return
	 */
	public boolean initDataVectors(int chunk) {
		if(!m_columnChunks.containsKey(chunk)) {
			logger.coding("cannot initialize data vectors. no chunk '" + chunk + "' available");
			return false;
		}
		
		for(RDataColumn column : m_columnChunks.get(chunk)) {	
			RType type = column.getType();
			column.initDataVector(m_numRows);			
		}		
		return true;
	}

	/**
	 * store data of a given row from all columns of a given chunk
	 * @param row
	 * @param rowIdx
	 * @param chunk
	 */
	public void addRowData(DataRow row, int rowIdx, int chunk) {
		
		for(RDataColumn column : m_columnChunks.get(chunk)) {
			DataCell cell = row.getCell(column.getIndex());
			column.addData(cell, rowIdx);
		}
	}

	/**
	 * transfer chunk to R
	 * @param chunk
	 * @param connection
	 * @param parName
	 * @param subExec
	 * @throws CanceledExecutionException
	 * @throws RserveException
	 */
	public void pushChunk(int chunk, RConnection connection, String parName, ExecutionMonitor subExec) throws CanceledExecutionException, RserveException {
		
		// create a new RList with a column vectors of this chunk
		RList rList = new RList(this.m_numRows, true);
		List<RDataColumn> columns = m_columnChunks.get(chunk);
    	for(RDataColumn col : columns) {
            String colName = col.getName();          
            rList.put(colName, col.getREXPData());
    	}
    	
    	// chunk name
    	String chunkName = parName + "_chunk_" + chunk;
    	m_chunkNames.add(chunkName);
    	
    	subExec.checkCanceled();
    	subExec.setMessage("transfer chunk " + chunk + " to R (cannot be cancelled)");
    	
    	// assign data to variable in R
    	logger.debug("transfer chunk " + chunkName + " to R");
    	connection.assign(chunkName, new REXPGenericVector(rList));
	}

	/**
	 * clears the data of all columns for a given chunk and call GC after that
	 * @param chunk
	 */
	public void clearChunk(int chunk) {
		List<RDataColumn> columns = m_columnChunks.get(chunk);
    	for(RDataColumn col : columns) {
    		col.clearData();
    	}
    	System.gc();
	}

	/**
	 * combines all transfered chunks into a single data frame
	 * fixes missing values
	 * @param parName
	 * @param connection
	 * @throws RserveException
	 */
	public void createDataFrame(String parName, RConnection connection) throws RserveException {
		logger.debug("combine chunks");

		String combineString = parName + " <- c(" + StringUtils.join(m_chunkNames, ",") + ")";
        String removeString = "rm(" + StringUtils.join(m_chunkNames, ",") + ")";
        
        if(m_numCols > 0) {
        	// combine chunks into one list
        	connection.voidEval(combineString);
        	// remove chunk objects
        	connection.voidEval(removeString);
            // convert list to dataframe
            // READABLE EXAMPLE:
            // attr(kIn,"row.names") <- .set_row_names(length(kIn[[1]])); 
            // class(kIn) <- "data.frame"; 
        	logger.debug("make dataframe");
            connection.voidEval("attr(" + parName + ", \"row.names\") <- .set_row_names(length(" + parName + "[[1]])); class(" + parName + ") <- \"data.frame\"; ");
        } else // create a data frame with a given number of rows but no columns
        	connection.voidEval(parName + " <- data.frame(matrix(nrow = " + m_numRows + ", ncol = 0))");
        
        if(m_numRows > 0) {
        	// push row names to R and assign to dataframe
        	connection.assign(parName + "_rownames", new REXPString(this.m_rowKeys));
        	connection.voidEval("rownames(" + parName + ") <- " + parName + "_rownames");
        	connection.voidEval("rm(" + parName + "_rownames)");
        }
        
        List<String> dataframeColumns = Arrays.asList(((REXPString)connection.eval("colnames(" + parName + ")")).asStrings());
        
        logger.debug("fix missing values");
        
        // update missing values for string columns
        // e.g. kIn[,2][c(1,6,20,21)] <- NA
        for(Integer chunk : m_columnChunks.keySet()) {
            for(RDataColumn col : m_columnChunks.get(chunk)) {
            	String missingIdxVec = "[c(" + col.getMissingIdx() + ")]";
            	connection.voidEval(parName + "[," + (dataframeColumns.indexOf(col.getName())+1) + "]" + missingIdxVec + " <- NA");
            }
        }
	}
	
}

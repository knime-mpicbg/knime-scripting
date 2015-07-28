package de.mpicbg.knime.scripting.r.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * <p>
 * table model for R <-> KNIME transfer
 * </p>
 * <pre>
 * General:
 * - create table, add column specs, add row keys, add data
 * </pre>
 * <p>
 * To R: push column chunks to R, clear chunk data, later combine within R to a single data frame
 * </p>
 * <p>
 * To KNIME: pull row chunks from R (re-use column data vector), fill in KNIME data table
 * </p>
 * 
 * @author Antje Janosch
 *
 */
public class RDataFrameContainer {
	
	/**
	 * column chunk number + list of columns in that chunk
	 */
	private LinkedHashMap<Integer, ArrayList<RDataColumn>> m_columnChunks = new LinkedHashMap<Integer, ArrayList<RDataColumn>>();
	
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
	 * @param levels 
	 */
/*	public void addColumnSpec(String cName, RType type, int chunk, int colIdx) {
		
		RDataColumn rColumn = new RDataColumn(cName, type, colIdx);
		
		addColumnSpec(rColumn, chunk);
	}*/

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
    	subExec.setMessage("transfer chunk " + (chunk+1) + " to R (cannot be cancelled)");
    	
    	// assign data to variable in R
    	logger.debug("transfer chunk " + chunkName + " to R");
    	connection.assign(chunkName, new REXPGenericVector(rList));
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
	 * adds a column to a certain chunk
	 * @param rColumn
	 * @param chunk
	 */
	public void addColumnSpec(RDataColumn rColumn, int chunk) {
		if(!m_columnChunks.containsKey(chunk))
			m_columnChunks.put(chunk, new ArrayList<RDataColumn>());
		m_columnChunks.get(chunk).add(rColumn);
	}

	/**
	 * @return KNIME table spec
	 */
	public DataTableSpec createDataTableSpec() {
		
		DataColumnSpec[] colSpecs = new DataColumnSpec[this.m_numCols];
		
		for(Entry<Integer, ArrayList<RDataColumn>> chunkSet : this.m_columnChunks.entrySet()) {
			for(RDataColumn rCol : chunkSet.getValue()) {
				DataColumnSpecCreator newColumn = new DataColumnSpecCreator(rCol.getName(), rCol.getKnimeDataType());
				DataColumnDomain domainInfo = rCol.getKnimeDomain(hasRows());
				newColumn.setDomain(domainInfo);
				colSpecs[rCol.getIndex()] = newColumn.createSpec();
			}
		}
		
		DataTableSpec tSpec = new DataTableSpec("Result from R", colSpecs);
		
		return tSpec;
	}

	/**
	 * pulls data frame with a given name from R (in row chunks of a given size) and fills KNIME table with this data
	 * @param con
	 * @param connection
	 * @param subExec
	 * @param rOutName
	 * @param rowChunkSize
	 * @throws RserveException
	 * @throws CanceledExecutionException 
	 */
	public void readDataFromR(BufferedDataContainer con, RConnection connection, ExecutionMonitor subExec, String rOutName, int rowChunkSize) throws RserveException, CanceledExecutionException {
		
		ArrayList<RDataColumn> cList = m_columnChunks.get(0);
		
		// get dataframe chunks (by n rows)
		int startRow = 1;
		//int rowIdx = 0;
		while(startRow <= m_numRows) {
			int endRow = startRow + rowChunkSize - 1;
			if(endRow > m_numRows) endRow = m_numRows;
			subExec.setMessage("retrieve rows " + startRow + " to " + endRow + " from R (cannot be cancelled)");
			subExec.checkCanceled();
			if(m_numCols > 0) {
				RList data = ((REXPGenericVector)connection.eval(rOutName + "[" + startRow + ":" + endRow + ",,drop = FALSE]")).asList();
				
				for(RDataColumn col : cList) {
					col.initDataVector(endRow - startRow);
					col.addData((REXP)data.get(col.getName()));
				}
			}
			
			subExec.setProgress((double)endRow/(double)m_numRows);
			
			int dataIdx = 0;
			for(int i = startRow; i <= endRow; i++) {
				DefaultRow row = new DefaultRow(m_rowKeys[i-1], getListOfCells(dataIdx));
				con.addRowToTable(row);
				dataIdx++;
			}
			
			startRow = endRow + 1;
		}	
		subExec.setProgress(1.0);
	}

	/**
	 * @param rowIdx
	 * @return data from a given row as KNIME cell list
	 */
	private List<DataCell> getListOfCells(int rowIdx) {
		List<DataCell> cells = new ArrayList<DataCell>();
		if(m_numCols > 0) {
			for(RDataColumn col : m_columnChunks.get(0)) {
				cells.add(col.getKNIMECell(rowIdx));
			}
		}
		return cells;
	}

	/**
	 * adds the row keys from a String vector
	 * @param rowNames
	 */
	public void addRowNames(String[] rowNames) {
		m_rowKeys = rowNames;
	}

	/**
	 * @return TRUE, if table contains at least one data row; FALSE otherwise
	 */
	public boolean hasRows() {
		return m_numRows > 0;
	}
	
}

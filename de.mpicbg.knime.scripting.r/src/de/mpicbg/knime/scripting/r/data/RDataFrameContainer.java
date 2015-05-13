package de.mpicbg.knime.scripting.r.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataCell;
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
import org.knime.core.node.NodeLogger;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class RDataFrameContainer {
	
	private List<Object> m_data = new ArrayList<Object>();;
	
	private TreeMap<Integer, DataType> m_converters = new TreeMap<Integer, DataType>();
	
	private HashMap<Integer, List<Integer>> m_missingFlags = new HashMap<Integer, List<Integer>>();
	
	private String[] m_rowKeys;
	
	private String[] m_columnNames;
	
	private int m_numRows;
	
	private int m_numCols;
	
	public static final String NA_VAL_FOR_R = "NA";
	
	private NodeLogger logger = NodeLogger.getLogger(RDataFrameContainer.class);

	public RDataFrameContainer(int numRows, int numCols) {
		super();
		this.m_numRows = numRows;
		this.m_numCols = numCols;
		
		m_rowKeys = new String[numRows];
		m_columnNames = new String[numCols];
	}

	public boolean addEmptyDataColumn(int idx, DataType colType, String colName) {
		
		// create empty data vector of the given type
		if (colType.isCompatible(IntValue.class)) {
            m_data.add(new int[m_numRows]);
        } else if (colType.isCompatible(DoubleValue.class)) {
            m_data.add(new double[m_numRows]);
        } else if (colType.isCompatible(StringValue.class)) {
            m_data.add(new String[m_numRows]);
            m_missingFlags.put(idx, new ArrayList<Integer>());
        } else {
        	logger.info("the R-container does not yet support the data type '" + colType.toString());
            return false;
        }
		
		// register data type for the given index and keep column name
		m_converters.put(idx, colType);
		m_columnNames[idx] = colName;
		
		return true;
	}

	
	public void addRowKey(int row, String rowKey) {
		m_rowKeys[row] = rowKey;
	}

	public Set<Integer> getPushableColumns() {
		return m_converters.keySet();
	}

	public boolean addData(int row, Integer col, DataCell cell) {
		
		if(row >= m_numRows) {
			logger.coding("Cannot write data to RDataFrameContainer as the row number " + row + " exceeds the number of rows (" + m_numRows + ")" );
			return false;
		}
		if(!m_converters.containsKey(col)) {
			logger.coding("Cannot write data to RDataFrameContainer as the column " + col + " is not registered" );
			return false;
		}
		
		DataType colType = m_converters.get(col);
		boolean missing = cell.isMissing();
		
		if (colType.isCompatible(IntValue.class)) {
            Integer value = missing ? REXPInteger.NA : ((IntCell)cell).getIntValue();
            int[] column = (int[]) m_data.get(col);
            column[row] = value;
        } else if (colType.isCompatible(DoubleValue.class)) {
            Double value = missing ? REXPDouble.NA : ((DoubleCell)cell).getDoubleValue();
            double[]column = (double[]) m_data.get(col);
            column[row] = value;
        } else if (colType.isCompatible(StringValue.class)) {
            String value = missing ? NA_VAL_FOR_R : ((StringCell)cell).getStringValue();
            if(missing)
            	m_missingFlags.get(col).add(row + 1);
            //TODO: is that still a bug?
            //value = value.trim().isEmpty() ? NA_VAL_FOR_R : value;
            String[] column = (String[]) m_data.get(col);
            column[row] = value;
        } else {
        	logger.info("the R-container does not yet support the data type '" + colType.toString());
        	return false;
        }
		
		return true;
	}

	public void pushToR(RConnection connection, ExecutionContext exec, String parName, int chunksize) throws RserveException, CanceledExecutionException {
		logger.info("Transfer knime -> R start: " + System.currentTimeMillis());

        // create the REXP (which is a list of vectors)        
        int batchCount = 0;
        
        List<RList> chunkLists = new ArrayList<RList>();
        List<String> chunkNames = new ArrayList<String>();
        
        // create chunks
        while(batchCount < m_numCols) {
        	RList rList = new RList(this.m_numRows, true);
        	for(int i = 0; i < chunksize && batchCount + i < m_numCols; i ++) {
        		int colIndex = batchCount + i;
        		DataType colType = m_converters.get(colIndex);
                String colName = m_columnNames[colIndex];
                
                if (colType.isCompatible(IntValue.class)) {
        			rList.put(colName, new REXPInteger((int[]) m_data.get(colIndex)));
                } else if (colType.isCompatible(DoubleValue.class)) {
                	rList.put(colName, new REXPDouble((double[]) m_data.get(colIndex)));
                } else if (colType.isCompatible(StringValue.class)) {
                	rList.put(colName, new REXPString((String[]) m_data.get(colIndex)));
                }
        	}
        	chunkLists.add(rList);
        	chunkNames.add(parName + "_chunk_" + batchCount);
        	batchCount += chunksize;
        }
        
        // push chunks to R
        for(int i = 0; i < chunkLists.size(); i++) {
        	String cName = chunkNames.get(i);
        	exec.checkCanceled();
        	exec.setMessage("transfer chunk " + i + " to R (cannot be cancelled)");
        	connection.assign(cName, new REXPGenericVector(chunkLists.get(i)));
        	exec.setProgress(i+1/chunkLists.size());
        }
        
        exec.setMessage("all chunks transfered");
        
        logger.debug("combine chunks");
        String combineString = parName + " <- c(" + StringUtils.join(chunkNames, ",") + ")";
        String removeString = "rm(" + StringUtils.join(chunkNames, ",") + ")";
        
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
        
        exec.setMessage("set rownames (cannot be cancelled)");
           
        if(m_numRows > 0) {
        	// push row names to R and assign to dataframe
        	connection.assign(parName + "_rownames", new REXPString(this.m_rowKeys));
        	connection.voidEval("rownames(" + parName + ") <- " + parName + "_rownames");
        	connection.voidEval("rm(" + parName + "_rownames)");
        }
        
        logger.debug("fix missing values");
        
        // update missing values for string columns
        // e.g. kIn[,2][c(1,6,20,21)] <- NA
        for(Integer col : m_missingFlags.keySet()) {
        	String missingIdxVec = "[c(" + StringUtils.join(m_missingFlags.get(col), ',') + ")]";
        	connection.voidEval(parName + "[," + (col+1) + "]" + missingIdxVec + " <- NA");
        }

        exec.setMessage("transfer to R finished");
        logger.info("Transfer knime -> R end: " + System.currentTimeMillis());
	}
	
}

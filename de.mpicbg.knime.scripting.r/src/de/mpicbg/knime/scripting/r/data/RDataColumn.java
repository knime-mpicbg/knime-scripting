package de.mpicbg.knime.scripting.r.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;

import de.mpicbg.knime.scripting.r.RUtils.RType;

/**
 * <p>
 * column model for R <-> KNIME transfer
 * </p>
 * <pre>
 * General:
 * - constructor, init data vector
 * </pre>
 * <pre>
 * R >>> KNIME:
 * - setLevels, add data, get REXP data, clear data 
 * </pre>
 * <pre>
 * KNIME >>> R:
 * - setLevels, set bounds, add data, get KNIME data type, get KNIME domain, get KNIME cell 
 * </pre>
 * 
 * @author Antje Janosch
 *
 */
public class RDataColumn {
	
	/** column index - zero based */
	private int m_idx;

	/** column name */
	private String m_name;
	
	/** column data type */
	private RType m_type;
	
	/** data vector */
	private Object[] m_data;
	
	/** list of missing value indicees - zero based*/
	private List<Integer> m_missingFlags = new ArrayList<Integer>();
	
	/** column levels(R)/domain values(KNIME) with index - zero-based */
	private HashMap<Integer, String> m_levels = new HashMap<Integer, String>();
	
	/** column upper and lower bound */
	private double[] m_bounds = new double[2];
	
	/**
	 * constructor
	 * @param name
	 * @param type
	 * @param idx
	 */
	public RDataColumn(String name, RType type, int idx){
		this.m_name = name;
		this.m_type = type;
		this.m_idx = idx;
	}
	
	/**
	 * assign new vector with a given size and column type
	 * vector type corresponds to type expected by REXP
	 * @param size
	 */
	public void initDataVector(int size) {
		switch(m_type) {
		case R_LOGICAL:
			m_data = new Byte[size];
			break;
		case R_INT:
			m_data = new Integer[size];
			break;
		case R_DOUBLE:
			m_data = new Double[size];
			break;
		case R_FACTOR:
			m_data = new Integer[size];
			break;
		case R_STRING:
			m_data = new String[size];
			break;
		default:
		}		
		
	}

	// GETTER / SETTER
	// ============================================================
	
	/**
	 * get data type
	 * @return
	 */
	public RType getType() {
		return m_type;
	}
	
	/** 
	 * get column index - zero based
	 * @return
	 */
	public int getIndex() {
		return m_idx;
	}
	
	/**
	 * get column name
	 * @return
	 */
	public String getName() {
		return m_name;
	}


	// KNIME >>> R
	// ============================================================

	/**
	 * add KNIME row value to data vector at a given row index
	 * also adds index to missing value indices if missing cekk
	 * @param cell
	 * @param rowIdx
	 */
	public void addData(DataCell cell, int rowIdx) {
		
		if(cell.isMissing() && m_type == RType.R_STRING)
			m_missingFlags.add(rowIdx);
		
		switch(m_type) {
		case R_LOGICAL:
			((Byte[])m_data)[rowIdx] = getLogicalValue(cell);
			break;
		case R_INT:
			((Integer[])m_data)[rowIdx] = getIntegerValue(cell);
			break;
		case R_DOUBLE:
			((Double[])m_data)[rowIdx] = getDoubleValue(cell);
			break;
		case R_FACTOR:
			((Integer[])m_data)[rowIdx] = getLevelIndex(cell);
			break;
		case R_STRING:
			((String[])m_data)[rowIdx] = getStringValue(cell);
			break;
		default:
		}
	}
	
	/**
	 * @param cell
	 * @return one-based integer representing the level-based string
	 */
	private Integer getLevelIndex(DataCell cell) {
		if(cell.isMissing())
			return REXPInteger.NA;
		
		String value = ((StringValue)cell).getStringValue();
		if(m_levels.containsValue(value)) {
			for(int i : m_levels.keySet())
				if(m_levels.get(i).equals(value))
					return (i+1);
		}
		return -1;
	}

	/**
	 * @param cell
	 * @return string value of KNIME cell or {@link RDataFrameContainer#NA_VAL_FOR_R} if missing cell
	 */
	private String getStringValue(DataCell cell) {
		String val;
		if(cell.isMissing())
			val = RDataFrameContainer.NA_VAL_FOR_R;
		else
			val = ((StringValue)cell).getStringValue();
		return val;
	}

	/**
	 * @param cell
	 * @return integer value of KNIME cell or {@link REXPInteger#NA} if missing cell
	 */
	private Integer getIntegerValue(DataCell cell) {
		int val;
		if(cell.isMissing())
			val = REXPInteger.NA;
		else
			val = ((IntValue)cell).getIntValue();
		return val;
	}

	/** 
	 * @param cell
	 * @return byte value of KNIME cell or {@link REXPLogical#NA} if missing cell
	 */
	private byte getLogicalValue(DataCell cell) {
		byte val;
		if(cell.isMissing())
			val = REXPLogical.NA;
		else
			val = (byte)(((BooleanValue)cell).getBooleanValue()? 1:0);
		return val;
	}

	/** 
	 * @param cell
	 * @return double value of KNIME cell or {@link REXPDouble#NA} if missing cell
	 */
	private double getDoubleValue(DataCell cell) {
		double val;
		if(cell.isMissing())
			val = REXPDouble.NA;
		else
			val = ((DoubleValue)cell).getDoubleValue();
		return val;
	}

	/**
	 * @return data vector as REXP representation
	 */
	public REXP getREXPData() {
		switch(m_type) {
		case R_LOGICAL:
			return new REXPLogical(ArrayUtils.toPrimitive((Byte[]) m_data));
		case R_INT:
			return new REXPInteger(ArrayUtils.toPrimitive((Integer[]) m_data));
		case R_DOUBLE:
			return new REXPDouble(ArrayUtils.toPrimitive((Double[]) m_data));
		case R_FACTOR:
			return new REXPFactor(ArrayUtils.toPrimitive((Integer[]) m_data), getLevels()); 
		case R_STRING:
			return new REXPString((String[]) m_data);
		default:
		}
		
		return null;
	}

	/**
	 * @return levels as String vector
	 */
	private String[] getLevels() {
		String[] levels = new String[m_levels.size()];
		Integer[] indices = m_levels.keySet().toArray(new Integer[m_levels.size()]);
		Arrays.sort(indices);
		
		for(int i : indices) {
			levels[i] = m_levels.get(i);
		}
		
		return levels;
	}

	/**
	 * set data vector to null
	 */
	public void clearData() {
		m_data = null;
	}

	/**
	 * @return comma separated string with missing value indices
	 */
	public String getMissingIdx() {
		return StringUtils.join(m_missingFlags, ',');
	}
	
	/**
	 * set levels from KNIME-domain values
	 * @param levels
	 */
	public void setLevels(Set<DataCell> levels) {
		m_levels = new HashMap<Integer, String>();
		assert levels!= null;
		int i = 0;
		for(DataCell cell : levels) {
			m_levels.put(i, ((StringValue)cell).getStringValue());
			i++;
		}
	}
	
	// R >>> KNIME
	// ============================================================

	/**
	 * set levels from String vector
	 * @param levels
	 */
	public void setLevels(String[] levels) {
		m_levels = new HashMap<Integer, String>();
		for(int i = 1; i <= levels.length; i++)
			this.m_levels.put(i, levels[i-1]);
	}
	
	/**
	 * set upper/lower bounds for numeric column
	 * @param bounds
	 */
	public void setBounds(double[] bounds) {
		this.m_bounds = bounds;
	}

	/**
	 * @return KNIME data type
	 */
	public DataType getKnimeDataType() {
		
		if(m_type.equals(RType.R_DOUBLE)) return DoubleCell.TYPE;
		if(m_type.equals(RType.R_INT)) return IntCell.TYPE;
		if(m_type.equals(RType.R_LOGICAL)) return BooleanCell.TYPE;
		if(m_type.equals(RType.R_FACTOR) || m_type.equals(RType.R_STRING)) return StringCell.TYPE;
		
		return null;
	}

	/**
	 * put levels into KNIME domain format (as {@link LinkedHashSet})
	 * @return
	 */
	private LinkedHashSet<DataCell> createDomainValueSet() {
		LinkedHashSet<DataCell> values = new LinkedHashSet<DataCell>();
		
		Integer[] indices = m_levels.keySet().toArray(new Integer[m_levels.size()]);
		Arrays.sort(indices);
		
		for(int i : indices) {
			String level = m_levels.get(i);
			values.add(new StringCell(level));
		}
		
		return values;
	}

	/**
	 * @param hasValues if TRUE, set levels
	 * @return KNIME domain object for this column
	 */
	public DataColumnDomain getKnimeDomain(boolean hasValues) {
		DataColumnDomainCreator dCDC = new DataColumnDomainCreator();
		if(hasValues) {
			if(m_type.equals(RType.R_DOUBLE)) {
				dCDC.setLowerBound(new DoubleCell(m_bounds[0]));
				dCDC.setUpperBound(new DoubleCell(m_bounds[1]));
			}
			if(m_type.equals(RType.R_INT)) {
				dCDC.setLowerBound(new IntCell((int) m_bounds[0]));
				dCDC.setUpperBound(new IntCell((int) m_bounds[1]));
			}
			if(m_type.equals(RType.R_LOGICAL)) {
				
				boolean lBound = m_bounds[0] != 0 ? true : false;
				boolean uBound = m_bounds[1] != 0 ? true : false;
				
				dCDC.setLowerBound(BooleanCell.get(lBound));
				dCDC.setUpperBound(BooleanCell.get(uBound));
				Set<BooleanCell> vals = new LinkedHashSet<BooleanCell>();
				vals.add(BooleanCell.get(uBound));
				vals.add(BooleanCell.get(lBound));
				dCDC.setValues(vals);
			}
			if(m_type.equals(RType.R_FACTOR)) {
				dCDC.setValues(createDomainValueSet());
			}
		}
		
		return dCDC.createDomain();
	}

	/**
	 * add REXP-vector values to data vector
	 * also keeps missing-value indices
	 * @param data
	 */
	public void addData(REXP data) {
		
		try {
			boolean[] missingVals = data.isNA();
			m_missingFlags = new ArrayList<Integer>();
			for(int i = 0; i < missingVals.length; i++)
				if(missingVals[i])
					m_missingFlags.add(i);
			
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		}
		
		switch(m_type) {
		case R_LOGICAL:
			m_data = ArrayUtils.toObject(((REXPLogical)data).isTRUE());
			break;
		case R_INT:
			m_data = ArrayUtils.toObject(((REXPInteger)data).asIntegers());
			break;
		case R_DOUBLE:
			m_data = ArrayUtils.toObject(((REXPDouble)data).asDoubles());
			break;
		case R_FACTOR:
			m_data = ((REXPFactor)data).asFactor().asStrings();
			break;
		case R_STRING:
			m_data = ((REXPString)data).asStrings();
			break;
		default:
		}
	}

	/** 
	 * @param rowIdx
	 * @return KNIME cell for a given row index
	 */
	public DataCell getKNIMECell(int rowIdx) {
		
		if(m_missingFlags.contains(rowIdx))
			return DataType.getMissingCell();
		
		switch(m_type) {
		case R_LOGICAL:
			return BooleanCell.get((boolean) m_data[rowIdx]);
		case R_INT:
			return new IntCell((Integer) m_data[rowIdx]);
		case R_DOUBLE:
			return new DoubleCell((Double) m_data[rowIdx]);
		case R_FACTOR:
		case R_STRING:
			return new StringCell((String) m_data[rowIdx]);
		default:
		}
		return null;
	}


}

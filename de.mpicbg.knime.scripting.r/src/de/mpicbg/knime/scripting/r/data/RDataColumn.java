package de.mpicbg.knime.scripting.r.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPString;

import de.mpicbg.knime.scripting.r.RUtils.RType;

public class RDataColumn {
	
	int m_idx;

	String m_name;
	
	RType m_type;
	
	Object[] m_data;
	
	List<Integer> m_missingFlags = new ArrayList<Integer>();
	
	HashMap<Integer, String> m_levels = new HashMap<Integer, String>();
	
	public RDataColumn(String name, RType type, int idx) {
		this.m_name = name;
		this.m_type = type;
		this.m_idx = idx;
	}

	public RType getType() {
		return m_type;
	}

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
			//break;
		case R_STRING:
			m_data = new String[size];
			break;
		default:
		}		
		
	}

	public int getIndex() {
		return m_idx;
	}

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
			//break;
		case R_STRING:
			((String[])m_data)[rowIdx] = getStringValue(cell);
			break;
		default:
		}
	}
	
	private String getStringValue(DataCell cell) {
		String val;
		if(cell.isMissing())
			val = RDataFrameContainer.NA_VAL_FOR_R;
		else
			val = ((StringValue)cell).getStringValue();
		return val;
	}

	private Integer getIntegerValue(DataCell cell) {
		int val;
		if(cell.isMissing())
			val = REXPInteger.NA;
		else
			val = ((IntValue)cell).getIntValue();
		return val;
	}

	private byte getLogicalValue(DataCell cell) {
		byte val;
		if(cell.isMissing())
			val = REXPLogical.NA;
		else
			val = (byte)(((BooleanValue)cell).getBooleanValue()? 1:0);
		return val;
	}

	private double getDoubleValue(DataCell cell) {
		double val;
		if(cell.isMissing())
			val = REXPDouble.NA;
		else
			val = ((DoubleValue)cell).getDoubleValue();
		return val;
	}

	public String getName() {
		return m_name;
	}

	public REXP getREXPData() {
		switch(m_type) {
		case R_LOGICAL:
			return new REXPLogical(ArrayUtils.toPrimitive((Boolean[]) m_data));
		case R_INT:
			return new REXPInteger(ArrayUtils.toPrimitive((Integer[]) m_data));
		case R_DOUBLE:
			return new REXPDouble(ArrayUtils.toPrimitive((Double[]) m_data));
		case R_FACTOR:
			//break;
		case R_STRING:
			return new REXPString((String[]) m_data);
		default:
		}
		
		return null;
	}

	public void clearData() {
		m_data = null;
	}

	public String getMissingIdx() {
		return StringUtils.join(m_missingFlags, ',');
	}

}

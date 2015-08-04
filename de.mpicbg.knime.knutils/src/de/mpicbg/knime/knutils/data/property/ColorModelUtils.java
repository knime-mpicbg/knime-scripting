package de.mpicbg.knime.knutils.data.property;

import java.awt.Color;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;

/**
 * The class provides methods around KNIME color model access
 * 
 * @author Antje Janosch
 *
 */
public class ColorModelUtils {

    /**
     * keys (domain values) and their color are extracted from the column spec
     * @param column spec
     * @return a map with assigned colors
     * @throws IllegalStateException
     */
	public static HashMap<DataCell, Color> parseNominalColorModel(
			DataColumnSpec columnSpec) {
		HashMap<DataCell, Color> colorMap = new HashMap<DataCell, Color>();
		
		ColorHandler ch = columnSpec.getColorHandler();
		DataColumnDomain domain = columnSpec.getDomain();
		// domain available?
		if (domain == null) {
			throw new IllegalStateException("No domain information available for column " + columnSpec.getName());
		}
		// domain contains domain values?
		if(!domain.hasValues()) {
			throw new IllegalStateException("No domain values available for column " + columnSpec.getName());
		}
		
		// extract domain values and their color
		for (DataCell cell : domain.getValues()) {
			Color c = ch.getColorAttr(cell).getColor();
			
			colorMap.put(cell, c);
		}
		
		return colorMap;
	}
	
	/**
	 * bounds and their color are extracted from the column spec
	 * @param column spec
	 * @return a map with assigned colors
	 * @throws IllegalStateException
	 */
	public static HashMap<DataCell, Color> parseNumericColorModel(DataColumnSpec columnSpec) {
		HashMap<DataCell, Color> colorMap = new HashMap<DataCell, Color>();
		
		ColorHandler ch = columnSpec.getColorHandler();
		DataColumnDomain domain = columnSpec.getDomain();
		// domain available?
		if (domain == null) {
			throw new IllegalStateException("No domain information available for column " + columnSpec.getName());
		}
		// domain contains bounds?
		if(!domain.hasBounds()) {
			throw new IllegalStateException("No bounds available for column " + columnSpec.getName());
		}
		if(!columnSpec.getType().isCompatible(DoubleValue.class))
			throw new IllegalStateException("Column type is not compatible to double values");
		
		DataCell lBound = domain.getLowerBound();
		DataCell uBound = domain.getUpperBound();
		Color lc = ch.getColorAttr(lBound).getColor();
		Color uc = ch.getColorAttr(uBound).getColor();
		
		colorMap.put(lBound, lc);
		colorMap.put(uBound, uc);
		
		return colorMap;
	}
	
	/**
	 * retrieves the index of the column with the color model attached
	 * @param tSpec
	 * @return column index or -1 if there was no column with color model
	 */
	public static int getColorColumn(DataTableSpec tSpec) {
		
		for(int i = 0; i < tSpec.getNumColumns(); i++) {
			if(tSpec.getColumnSpec(i).getColorHandler() != null)
				return i;
		}
		
		return -1;
	}

	/**
	 * checks, if the column of that table has nominal domain values
	 * @param tSpec
	 * @param colorIdx
	 * @return true, if the column contains nominal domain values
	 */
	public static boolean isNominal(DataTableSpec tSpec, int colorIdx) {
		return tSpec.getColumnSpec(colorIdx).getDomain().hasValues();
	}
	
	/**
	 * checks, if the column of that table has lower/upper bounds defined
	 * @param tSpec
	 * @param colorIdx
	 * @return true, if the column has lower/upper bounds defined
	 */
	public static boolean isNumeric(DataTableSpec tSpec, int colorIdx) {
		return tSpec.getColumnSpec(colorIdx).getDomain().hasBounds();
	}
}

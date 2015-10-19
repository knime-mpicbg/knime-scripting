package de.mpicbg.knime.knutils.data.property;

import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ShapeFactory.Shape;
import org.knime.core.data.property.ShapeHandler;

/**
 * The class provides methods around KNIME shape model
 * 
 * @author Antje Janosch
 *
 */
public class ShapeModelUtils {
	
	/**
	 * retrieves the index of the column with the shape model attached
	 * @param tSpec
	 * @return column index or -1 if there was no column with size model
	 */
	public static int getShapeColumn(DataTableSpec tSpec) {
		
		for(int i = 0; i < tSpec.getNumColumns(); i++) {
			if(tSpec.getColumnSpec(i).getShapeHandler() != null)
				return i;
		}
		
		return -1;
	}

	/**
	 * keys (domain values) and their shapes are extracted from the column spec
	 * @param columnSpec
	 * @return a map of data cells (keys) and their shape (as {@link org.knime.core.data.property.ShapeFactory.Shape})
	 */
	public static HashMap<DataCell, Shape> parseNominalShapeModel(
			DataColumnSpec columnSpec) {
		HashMap<DataCell, Shape> shapeMap = new HashMap<DataCell, Shape>();
		
		ShapeHandler ch = columnSpec.getShapeHandler();
		DataColumnDomain domain = columnSpec.getDomain();
		// domain available?
		if (domain == null) {
			throw new IllegalStateException("No domain information available for column " + columnSpec.getName());
		}
		// domain contains domain values?
		if(!domain.hasValues()) {
			throw new IllegalStateException("No domain values available for column " + columnSpec.getName());
		}
		
		// extract domain values and their shape
		for (DataCell cell : domain.getValues()) {
			Shape s = ch.getShape(cell);		
			shapeMap.put(cell, s);
		}
		
		return shapeMap;
	}
}

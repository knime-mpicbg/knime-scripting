package de.mpicbg.knime.knutils.data.property;

import org.knime.core.data.DataTableSpec;

/**
 * The class provides methods around KNIME size model
 * 
 * @author Antje Janosch
 *
 */
public class SizeModelUtils {

	/**
	 * retrieves the index of the column with the size model attached
	 * @param tSpec
	 * @return column index or -1 if there was no column with size model
	 */
	public static int getSizeColumn(DataTableSpec tSpec) {
		
		for(int i = 0; i < tSpec.getNumColumns(); i++) {
			if(tSpec.getColumnSpec(i).getSizeHandler() != null)
				return i;
		}	
		return -1;
	}
}

package de.mpicbg.knime.knutils.data.property;

import org.knime.core.data.DataTableSpec;

public class SizeModelUtils {

	public static int getSizeColumn(DataTableSpec tSpec) {
		
		for(int i = 0; i < tSpec.getNumColumns(); i++) {
			if(tSpec.getColumnSpec(i).getSizeHandler() != null)
				return i;
		}
		
		return -1;
	}
}

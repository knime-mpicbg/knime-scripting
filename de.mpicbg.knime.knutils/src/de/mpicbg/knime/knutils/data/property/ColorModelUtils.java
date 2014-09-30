package de.mpicbg.knime.knutils.data.property;

import java.awt.Color;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
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
	 * checks whether color model is for KNIME nominal colors
	 * @param model
	 * @return true, if it is a nominal color model
	 * @throws InvalidSettingsException
	 */
    public static boolean isNominalKnimeColor(ModelContent model) throws InvalidSettingsException {
    	String colorModelClass = model.getString("color_model_class");
    	return colorModelClass.contains(ColorModelNominal.class.getName());
	}

    /**
     * keys (domain values) and their color is extracted from the column spec
     * @param column spec
     * @return a map for assigned colors
     * @throws InvalidSettingsException
     */
	public static HashMap<String, Color> parseNominalColorModel(
			DataColumnSpec columnSpec) {
		HashMap<String, Color> colorMap = new HashMap<String, Color>();
		
		ColorHandler ch = columnSpec.getColorHandler();
		if (columnSpec.getDomain() == null) {
			throw new IllegalStateException("Not a nominal column");
		}
		if(!columnSpec.getType().isCompatible(StringValue.class)) {
			throw new IllegalStateException("Column type is not compatible to string values");
		}
		for (DataCell cell : columnSpec.getDomain().getValues()) {
			Color c = ch.getColorAttr(cell).getColor();
			
			colorMap.put(((StringCell)cell).getStringValue(), c);
		}
		
		return colorMap;
	}
}

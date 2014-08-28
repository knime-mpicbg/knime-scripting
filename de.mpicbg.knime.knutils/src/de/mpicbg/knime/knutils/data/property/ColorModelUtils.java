package de.mpicbg.knime.knutils.data.property;

import java.awt.Color;
import java.util.HashMap;
import java.util.Set;

import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigEntries;

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
     * keys (domain values) and their color is extracted from the model
     * @param model
     * @return a map for assigned colors
     * @throws InvalidSettingsException
     */
	public static HashMap<String, Color> parseNominalColorModel(ModelContent model) throws InvalidSettingsException {    	
	
    	Config colorCfg = model.getConfig("color_model");
    	Set<String> keys = colorCfg.keySet();
    	HashMap<String, Color> colorMap = new HashMap<String, Color>();
    	
    	for(String key : keys) {
    		AbstractConfigEntry cfgEntry = colorCfg.getEntry(key);
    		if(!cfgEntry.getType().equals(ConfigEntries.config)) {
    			colorMap.put(key, new Color(colorCfg.getInt(key)));
    		}
    	}
    	return colorMap;
	}
}

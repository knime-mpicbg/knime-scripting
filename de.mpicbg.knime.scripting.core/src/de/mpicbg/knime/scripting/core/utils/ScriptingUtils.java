package de.mpicbg.knime.scripting.core.utils;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import de.mpicbg.knime.scripting.core.TemplateCache;

/**
 * Helper class for static methods
 * @author Antje Jansoch
 *
 */
public class ScriptingUtils {
	
	/**
	 * add templates to template-cache-singleton
	 * @param templateStrings
	 * @param path
	 */
	public static void loadTemplateCache(List<String> templateStrings, String PLUGIN_ID) {
        TemplateCache cache = TemplateCache.getInstance();
        
        for(String prefString : templateStrings) {
	        try {
				cache.addTemplatesFromPref(prefString, getBundlePath(PLUGIN_ID).toOSString());
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	/**
	 * retrieves the path of a bundle with the given plugin-id
	 * @param PLUGIN_ID
	 * @return
	 */
    public static IPath getBundlePath(String PLUGIN_ID) {
    	Bundle bundle = Platform.getBundle(PLUGIN_ID);
    	IPath path = Platform.getStateLocation(bundle);
    	return path;
    }
}

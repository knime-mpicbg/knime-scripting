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
	 * @param bundle
	 */
	public static void loadTemplateCache(List<String> templateStrings, Bundle bundle) {
        IPath path = Platform.getStateLocation(bundle);
        TemplateCache cache = TemplateCache.getInstance();
        
        for(String prefString : templateStrings) {
	        try {
				cache.addTemplatesFromPref(prefString, path);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
}

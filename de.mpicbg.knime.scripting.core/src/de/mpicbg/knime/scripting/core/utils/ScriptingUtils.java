package de.mpicbg.knime.scripting.core.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	
	public static final String LOCAL_CACHE_FOLDER = "template_cache";
	
	/**
	 * add templates to template-cache-singleton
	 * @param templateStrings
	 * @param path
	 */
	public static void loadTemplateCache(List<String> templateStrings, Bundle bundle) {
        TemplateCache cache = TemplateCache.getInstance();
        
        String bundlePath = getBundlePath(bundle).toOSString();
        
        Path cacheFolder = Paths.get(bundlePath, LOCAL_CACHE_FOLDER);
        Path indexFile = Paths.get(bundlePath, LOCAL_CACHE_FOLDER, "tempFiles.index");		
        
        for(String prefString : templateStrings) {
	        try {
				cache.addTemplatesFromPref(prefString, cacheFolder, indexFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	/**
	 * retrieves the path of a bundle with the given plugin-id
	 * @param bundle
	 * @return
	 */
    public static IPath getBundlePath(Bundle bundle) {
    	IPath path = Platform.getStateLocation(bundle);
    	return path;
    }
}

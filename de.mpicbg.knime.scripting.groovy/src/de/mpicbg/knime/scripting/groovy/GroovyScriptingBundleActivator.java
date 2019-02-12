/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package de.mpicbg.knime.scripting.groovy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.groovy.prefs.GroovyScriptingPreferenceInitializer;


/**
 * This is the eclipse bundle activator. Note: KNIME node developers probably won't have to do anything in here, as this
 * class is only needed by the eclipse platform/plugin mechanism. If you want to move/rename this file, make sure to
 * change the plugin.xml file in the project root directory accordingly.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class GroovyScriptingBundleActivator extends AbstractUIPlugin {

    /**
     * Make sure that this *always* matches the ID in plugin.xml.
     */
    public static final String PLUGIN_ID = "de.mpicbg.tds.knime.scripting.groovy";

    // The shared instance.
    private static GroovyScriptingBundleActivator plugin;


    /**
     * The constructor.
     */
    public GroovyScriptingBundleActivator() {
        plugin = this;
    }


    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Plugin
     */
    public static GroovyScriptingBundleActivator getDefault() {
        return plugin;
    }


	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
        // get bundle and template prefstrings and load them into template cache
        try {

        	Bundle bundle = FrameworkUtil.getBundle(getClass());

        	List<String> preferenceStrings = new ArrayList<String>();
        	IPreferenceStore prefStore = GroovyScriptingBundleActivator.getDefault().getPreferenceStore();
        	preferenceStrings.add(prefStore.getString(GroovyScriptingPreferenceInitializer.GROOVY_TEMPLATE_RESOURCES));

        	ScriptingUtils.loadTemplateCache(preferenceStrings, bundle);
        } catch(Exception e) {
        	NodeLogger logger = NodeLogger.getLogger("scripting template init");
        	logger.coding(e.getMessage());
        }
	}

}
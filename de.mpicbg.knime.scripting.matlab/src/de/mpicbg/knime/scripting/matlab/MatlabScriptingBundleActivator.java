/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package de.mpicbg.knime.scripting.matlab;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * This is the eclipse bundle activator. Note: KNIME node developers probably won't have to do anything in here, as this
 * class is only needed by the eclipse platform/plugin mechanism. If you want to move/rename this file, make sure to
 * change the plugin.xml file in the project root directory accordingly.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class MatlabScriptingBundleActivator extends AbstractUIPlugin {

    /**  Make sure that this *always* matches the ID in plugin.xml */
    public static final String PLUGIN_ID = "de.mpicbg.knime.scripting.matlab";

    /** The shared instance */
    private static MatlabScriptingBundleActivator plugin;
    
    /**
     * set to true if there has been one attempt to load the template cache
     */
    public static boolean hasTemplateCacheLoaded = false;


    /**
     * The constructor.
     */
    public MatlabScriptingBundleActivator() {
        plugin = this;
    }


    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }


    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Plugin
     */
    public static MatlabScriptingBundleActivator getDefault() {
        return plugin;
    }
}

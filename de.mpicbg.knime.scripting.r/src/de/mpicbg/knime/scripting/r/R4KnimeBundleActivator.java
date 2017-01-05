/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package de.mpicbg.knime.scripting.r;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.TemplateCache;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * This is the eclipse bundle activator. Note: KNIME node developers probably won't have to do anything in here, as this
 * class is only needed by the eclipse platform/plugin mechanism. If you want to move/rename this file, make sure to
 * change the plugin.xml file in the project root directory accordingly.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class R4KnimeBundleActivator extends AbstractUIPlugin {

    /**
     * Make sure that this *always* matches the ID in plugin.xml.
     */
    public static final String PLUGIN_ID = "de.mpicbg.tds.knime.scripting.r";

    // The shared instance.
    private static R4KnimeBundleActivator plugin;


    /**
     * The constructor.
     */
    public R4KnimeBundleActivator() {
        super();
        plugin = this;
    }


    /**
     * This method is called upon plug-in activation.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        
        // get bundle and template prefstrings and load them into template cache
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        List<String> preferenceStrings = new ArrayList<String>();
        IPreferenceStore prefStore = R4KnimeBundleActivator.getDefault().getPreferenceStore();
        preferenceStrings.add(prefStore.getString(RPreferenceInitializer.R_SNIPPET_TEMPLATES));
        preferenceStrings.add(prefStore.getString(RPreferenceInitializer.R_PLOT_TEMPLATES));
        
        ScriptingUtils.loadTemplateCache(preferenceStrings, bundle);
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
    public static R4KnimeBundleActivator getDefault() {
        return plugin;
    }

}


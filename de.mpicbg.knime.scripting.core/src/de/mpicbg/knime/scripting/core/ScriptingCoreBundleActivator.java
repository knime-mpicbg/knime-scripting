package de.mpicbg.knime.scripting.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;

public class ScriptingCoreBundleActivator extends AbstractUIPlugin {

	/**
     * Make sure that this *always* matches the ID in plugin.xml.
     */
    public static final String PLUGIN_ID = "de.mpicbg.knime.scripting.core";
    
    // The shared instance.
    private static ScriptingCoreBundleActivator plugin;
    
    /**
     * constructor
     */
    public ScriptingCoreBundleActivator() {
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
    public static ScriptingCoreBundleActivator getDefault() {
        return plugin;
    }

}

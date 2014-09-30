package de.mpicbg.knime.scripting.matlab.srv;

import org.eclipse.ui.plugin.AbstractUIPlugin;


public class MatlabServerBundleActivator extends AbstractUIPlugin {
	
	/** Make sure that this *always* matches the ID in plugin.xml */
	public static final String PLUGIN_ID = "de.mpicbg.knime.scripting.matlab.srv";
	/** Shared instance */
	private static MatlabServerBundleActivator plugin;

	/**
	 * Constructor
	 */
	public MatlabServerBundleActivator() {
		plugin = this;
	}
	
	/**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Plugin
     */
    public static MatlabServerBundleActivator getDefault() {
        return plugin;
    }

}

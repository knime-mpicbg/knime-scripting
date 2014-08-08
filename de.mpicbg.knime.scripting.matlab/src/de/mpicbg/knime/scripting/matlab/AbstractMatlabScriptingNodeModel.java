/*
 * Copyright (c) 2011. 
 * Max Planck Institute of Molecular Cell Biology and Genetics, Dresden
 *
 * This module is distributed under the BSD-License. For details see the license.txt.
 *
 * It is the obligation of every user to abide terms and conditions of The MathWorks, Inc. Software License Agreement. In particular Article 8 “Web Applications”: it is permissible for an Application created by a Licensee of the NETWORK CONCURRENT USER ACTIVATION type to use MATLAB as a remote engine with static scripts.
 */

package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;
import de.mpicbg.knime.scripting.matlab.srv.MatlabClient;
import matlabcontrol.MatlabConnectionException;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.port.PortType;


/**
 * 
 * @author Holger Brandl, Felix Meyenhofer
 *
 */
public abstract class AbstractMatlabScriptingNodeModel extends AbstractScriptingNodeModel {

    /** Settings from the KNIME preference dialog */
    protected IPreferenceStore preferences = MatlabScriptingBundleActivator.getDefault().getPreferenceStore();

    /** Holds the MATLAB client object */
    protected MatlabClient matlab; 
    
    /** MALTLAB type */
    protected String type;

    
    /**
     * Constructor
     * 
     * @param inPorts
     * @param outPorts
     * @throws MatlabConnectionException 
     */
    protected AbstractMatlabScriptingNodeModel(PortType[] inPorts, PortType[] outPorts, boolean useNewSettingsHashmap) {
        super(inPorts, outPorts, useNewSettingsHashmap);
        
        // Get the values from the KNIME preference dialog.
        boolean local = preferences.getBoolean(MatlabPreferenceInitializer.MATLAB_LOCAL);
        String host = preferences.getString(MatlabPreferenceInitializer.MATLAB_HOST);
        int port = preferences.getInt(MatlabPreferenceInitializer.MATLAB_PORT);
        
        // Initialize the MATLAB client
        initializeMatlabClient(local, host, port);
        
        // Add a property change listener that re-initializes the MATLAB client if the local flag changes.
        preferences.addPropertyChangeListener(new IPropertyChangeListener() {
			boolean newLocal = preferences.getBoolean(MatlabPreferenceInitializer.MATLAB_LOCAL);
			String newHost = preferences.getString(MatlabPreferenceInitializer.MATLAB_HOST);
	        int newPort = preferences.getInt(MatlabPreferenceInitializer.MATLAB_PORT);
	        
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				String newValue = event.getNewValue().toString();
				
				if (event.getProperty() == MatlabPreferenceInitializer.MATLAB_LOCAL) {
					newLocal = Boolean.parseBoolean(newValue);
					logger.info("MATLAB: server property (MATLAB_LOCAL) was changed to " + newLocal + ". Re-initializing MATLAB client.");
				} else if (event.getProperty() == MatlabPreferenceInitializer.MATLAB_HOST) {
					String newHost = newValue;
					logger.info("MATLAB: server property (MATLAB_HOST) was changed to " + newHost + ". Re-initializing MATLAB client.");
				} else if (event.getProperty() == MatlabPreferenceInitializer.MATLAB_PORT) {
					newPort = Integer.parseInt(newValue);
					logger.info("MATLAB: server property (MATLAB_PORT) was changed to " + newPort + ". Re-initializing MATLAB client.");
				}
				
				initializeMatlabClient(newLocal, newHost, newPort);
			}
		});
    }
    
    
    /**
     * Initialize the MATLAB client
     * 
     * @param local Flag to choose local or remote execution
     * @param port 
     * @param host 
     */
    private void initializeMatlabClient(boolean local, String host, int port) {
        try {
			matlab = new MatlabClient(local, host, port);
		} catch (MatlabConnectionException e) {
			logger.error("MATLAB could not be started. You have to install MATLAB on you computer" +
					" to use KNIME's MATLAB scripting integration.");
			e.printStackTrace();
		}
    }
    
}

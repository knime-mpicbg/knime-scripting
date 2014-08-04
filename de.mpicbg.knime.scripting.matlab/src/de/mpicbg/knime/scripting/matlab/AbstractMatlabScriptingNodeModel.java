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
        
        // Get the flag from the preference pane
        boolean local = preferences.getBoolean(MatlabPreferenceInitializer.MATLAB_LOCAL);
        
        // Add a property change listener that re-initializes the MATLAB client if the local flag changes.
        preferences.addPropertyChangeListener(new IPropertyChangeListener() {
			String flag;
			boolean newlocal;
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == MatlabPreferenceInitializer.MATLAB_LOCAL) {
					flag = event.getNewValue().toString();
					newlocal = (flag == "true") ? true : false;
					logger.info("MATLAB: server property (MATLAB_LOCAL) was changed to " + flag + ". Re-initializing MATLAB client.");
					initializeMatlabClient(newlocal);
				}
			}
		});
        
        // Initialize the MATLAB client
        initializeMatlabClient(local);
    }
    
    
    /**
     * Initialize the Matlab client
     * 
     * @param local Flag to choose local or remote execution
     */
    private void initializeMatlabClient(boolean local) {
        try {
			matlab = new MatlabClient(local);
		} catch (MatlabConnectionException e) {
			logger.error("MATLAB could not be started. You have to install MATLAB on you computer" +
					" to use KNIME's MATLAB scripting integration.");
			e.printStackTrace();
		}
    }
    
}

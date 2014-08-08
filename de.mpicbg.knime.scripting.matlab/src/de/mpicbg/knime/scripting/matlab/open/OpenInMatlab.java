package de.mpicbg.knime.scripting.matlab.open;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.matlab.MatlabScriptingBundleActivator;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;
import de.mpicbg.knime.scripting.matlab.srv.MatlabClient;
import matlabcontrol.MatlabConnectionException;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;


/**
 * Node to open a KNIME table in MATLAB
 * 
 * @author Felix Meyenhofer
 */
public class OpenInMatlab extends AbstractNodeModel {
	
    /** Object to access the MATLAB session */
    private MatlabClient matlab;
    
    /** MATLAB type */
    private String type;

    /**
     * Constructor for the node model.
     */
    public OpenInMatlab() {
    	// Define the ports and use a hash-map for setting models 
        super(1, 0, true);
        
        // Instantiate the local MATLAB server      
        try {
			matlab = new MatlabClient(true);
		} catch (MatlabConnectionException e) {
			logger.error("MATLAB could not be started. You have to install MATLAB on you computer" +
					" to use KNIME's MATLAB scripting integration.");
			e.printStackTrace();
		}
    }   


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        // Get the table
        BufferedDataTable data = inData[0];
        
        type = MatlabScriptingBundleActivator.getDefault()
        		.getPreferenceStore()
        		.getString(MatlabPreferenceInitializer.MATLAB_TYPE);
        
        try {               	
        	// Execute the command in MATLAB
        	matlab.client.openTask(data, this.type);
        	exec.checkCanceled();
        	
        	// Housekeeping
        	matlab.cleanup();

        } catch (Exception e) {
        	throw e;
    	} finally {
    		this.matlab.rollback(); // Double check if the proxy was returned (in case of an Exception it will happen here)
    	}
        
        logger.info("The data is now loaded in MATLAB. Switch to the MATLAB command window.");

        return new BufferedDataTable[0];
    }

}
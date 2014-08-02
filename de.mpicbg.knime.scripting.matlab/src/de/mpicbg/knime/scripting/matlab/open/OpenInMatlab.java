package de.mpicbg.knime.scripting.matlab.open;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.matlab.srv.MatlabClient;
import matlabcontrol.MatlabConnectionException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * Node to open a KNIME table in MATLAB
 * 
 * @author Felix Meyenhofer
 */
public class OpenInMatlab extends AbstractNodeModel {

	/** KNIME setting name for the MATLAB variable type */
	static String MATLAB_TYPE_SETTING_NAME = "matlab.type";

    /** Object to access the MATLAB session */
    private MatlabClient matlab;
    

    /**
     * Constructor for the node model.
     */
    public OpenInMatlab() {
    	// Define the ports and use a hash-map for setting models 
        super(1, 0, true);
        
        // Instantiate settings
        addModelSetting(MATLAB_TYPE_SETTING_NAME, createMatlabTypeSetting());
        
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
        
        try {        
        	// Generate the string that will be evaluated in MATLAB
        	String type = ((SettingsModelString)getModelSetting(MATLAB_TYPE_SETTING_NAME)).getStringValue();
        	exec.checkCanceled();
        	
        	// Execute the command in MATLAB
        	matlab.client.openTask(data, type);
        	exec.checkCanceled();
        	
        	// Housekeeping
        	matlab.cleanup();
        } catch (CanceledExecutionException e) {
    		throw e;
    	} catch (InterruptedException e) {
    		throw e;    		
    	} finally {
    		this.matlab.rollback();
    	}
        
        logger.info("The data is now loaded in MATLAB. Switch to the MATLAB command window.");

        return new BufferedDataTable[0];
    }
    
    /**
     * Create the SettingModel for the MATLAB type
     * 
     * @return MATLAB type Setting
     */
     static SettingsModelString createMatlabTypeSetting() {
        return new SettingsModelString(MATLAB_TYPE_SETTING_NAME, "dataset");
    }

}
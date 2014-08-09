package de.mpicbg.knime.scripting.matlab.snippet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

import de.mpicbg.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;
import de.mpicbg.knime.scripting.matlab.srv.Matlab;


/**
 * This is the model implementation of MatlabSnippet. MATLAB integration for KNIME.
 *
 * @author Holger Brandl, Felix Meyenhofer
 */
public class MatlabSnippetNodeModel extends AbstractMatlabScriptingNodeModel {

    /**
     * Constructor for the node model.
     */
    protected MatlabSnippetNodeModel() {
        super(createPorts(1), createPorts(1), true);
    }

    
    /** 
     * {@inheritDoc}
     */
    @Override
    public String getDefaultScript() {
        return Matlab.DEFAULT_SNIPPET;
    }


    /** 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{null};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {

    	BufferedDataTable[] outData = new BufferedDataTable[1];
    	
    	try {
            // Get preference pane properties
            this.type = preferences.getString(MatlabPreferenceInitializer.MATLAB_TYPE);
            this.method = preferences.getString(MatlabPreferenceInitializer.MATLAB_TRANSFER_METHOD);
            
            // Get the code
    		String snippet = prepareScript();
    		exec.checkCanceled();
    		
    		// Execute it
    		BufferedDataTable table = this.matlab.client.snippetTask(inData[0], this.method, exec, snippet, this.type);
    		outData[0] = table;
    		exec.checkCanceled();
    		
    		// Housekeeping
    		this.matlab.cleanup();
    		exec.checkCanceled();

    	} catch (Exception e) {
    		throw e;
    	} finally {
    		this.matlab.rollback(); // Double check if the proxy was returned (in case of an Exception it will happen here)
    	}
    	
    	return outData;
    }

}

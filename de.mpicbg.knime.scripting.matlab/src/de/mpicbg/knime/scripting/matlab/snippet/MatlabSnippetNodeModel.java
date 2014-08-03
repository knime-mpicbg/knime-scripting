package de.mpicbg.knime.scripting.matlab.snippet;

import java.lang.InterruptedException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
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
            // Get the MATLAB type
            this.type = preferences.getString(MatlabPreferenceInitializer.MATLAB_TYPE);
            
            // Get the code
    		String snippet = prepareScript();
    		exec.checkCanceled();
    		
    		// Execute it
    		BufferedDataTable table = this.matlab.client.snippetTask(inData[0], exec, snippet, this.type);
    		outData[0] = table;
    		exec.checkCanceled();
    		
    		// Housekeeping
    		this.matlab.cleanup();
    		exec.checkCanceled();
    	} catch (CanceledExecutionException e) {
    		throw e;
    	} catch (InterruptedException e) {
    		throw e;    		
    	} finally {
    		this.matlab.rollback();
    	}
    	
    	return outData;
    }

}

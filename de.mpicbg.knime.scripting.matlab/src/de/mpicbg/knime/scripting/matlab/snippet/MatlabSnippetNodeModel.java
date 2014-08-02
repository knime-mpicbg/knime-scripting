package de.mpicbg.knime.scripting.matlab.snippet;

import java.lang.InterruptedException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

import de.mpicbg.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;
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
    		String snippet = prepareScript();
    		exec.checkCanceled();
    		
    		BufferedDataTable table = this.matlab.client.snippetTask(inData[0], exec, snippet, Matlab.DEFAULT_TYPE);
    		this.matlab.cleanup();
    		exec.checkCanceled();
    		
    		outData[0] = table;
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

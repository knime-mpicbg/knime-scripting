package de.mpicbg.knime.scripting.r.node.generic.converttotable;

import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * Model: A generic R node which pulls a an R data frame and returns it as KNIME table
 *
 * @author Holger Brandl, Antje Janosch
 */
public class ConvertToTableNodeModel2 extends AbstractScriptingNodeModel {

	/**
	 * constructor
	 */
    public ConvertToTableNodeModel2() {
        super(createPorts(1, RPortObject.TYPE, RPortObject.class), createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class), true, true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	return new PortObjectSpec[]{(DataTableSpec) null};
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		
		boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);
		int chunkOutSize = ((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_OUT)).getIntValue();
		
        RConnection connection = RUtils.createConnection();
        BufferedDataTable dataTable = null;
        //File rWorkspaceFile = null;

        try {
	        // 1) restore the workspace in a different server session
	        RUtils.pushToR(inData, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);
	
	        // 2) run the script  (remove all linebreaks and other no space whitespace-characters
	        String script = prepareScript();
	        String fixedScript = fixEncoding(script);
	        
	        RUtils.parseScript(connection, fixedScript);
	        
	        if(useEvaluate) {
	        	// parse and run script
	        	// evaluation list, can be used to create a console view, throws first R-error-message
	        	REXPGenericVector knimeEvalObj = RUtils.evaluateScript(fixedScript, connection);
	        	// check for warnings
	        	ArrayList<String> warningMessages = RUtils.checkForWarnings(connection);
	        	if(warningMessages.size() > 0) setWarningMessage("R-script produced " + warningMessages.size() + " warnings. See R-console view for further details");
	        	
	
	        } else {
	        	// parse and run script
	        	RUtils.evalScript(connection, fixedScript);     	
	        }

	        // check if result data frame is present
	    	if( ((REXPLogical) connection.eval("exists(\"" + RSnippetNodeModel.R_INVAR_BASE_NAME + "\")")).isFALSE()[0] ) 
	    		throw new KnimeScriptingException("R workspace does not contain " + RSnippetNodeModel.R_INVAR_BASE_NAME + " after execution.");    	
	        REXP out = connection.eval(RSnippetNodeModel.R_INVAR_BASE_NAME);
	        if(!out.inherits("data.frame")) 
	        	throw new KnimeScriptingException(RSnippetNodeModel.R_INVAR_BASE_NAME + " is not a data frame");
	        
	        // extract output data-frame from R
	        dataTable = RUtils.pullTableFromR(RSnippetNodeModel.R_INVAR_BASE_NAME, connection, exec, chunkOutSize);
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }
        connection.close();

        return new BufferedDataTable[]{dataTable};
	}

    /**
     * {@inheritDoc}
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec) throws KnimeScriptingException {
		try {
			String rawScript = prepareScript();
			RUtils.openInR(inData, exec, rawScript, logger);   
			setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
		} catch (REXPMismatchException | IOException | REngineException e) {
			throw new KnimeScriptingException("Failed to open in R\n" + e);
		}	
	}
}

package de.mpicbg.knime.scripting.r.node.generic.converttotable;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.port.RPortObject;


/**
 * Model: A generic R node which pulls a an R data frame and returns it as KNIME table
 *
 * @author Holger Brandl, Antje Janosch
 */
public class ConvertToTableNodeModel2 extends AbstractRScriptingNodeModel {
	
	public static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, RPortObject.TYPE, RPortObject.class),				// 1 generic input
			createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class),	// 1 KNIME table output
			new RColumnSupport(), 
			true, 		// use script
			true, 		// provide open in R
			true);		// use chunks
	
	public static final String GENERIC_TOTABLE_DFT = "rOut <- iris";

	/**
	 * constructor
	 */
    public ConvertToTableNodeModel2() {
        super(nodeModelConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	return new PortObjectSpec[]{(DataTableSpec) null};
    }

	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		super.executeImpl(inData, exec);
		runScript(exec);
		return pullOutputFromR(exec);
	}

	@Override
	public String getDefaultScript() {
		return GENERIC_TOTABLE_DFT;
	}
	
	

    /**
     * {@inheritDoc}
     */
	/*@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		
		boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);
		int chunkOutSize = ((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_OUT)).getIntValue();
		
        RConnection connection = RUtils.createConnection();
        BufferedDataTable dataTable = null;
        //File rWorkspaceFile = null;

        try {
	        // 1) restore the workspace in a different server session
	        //pushToR(inData, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);
	
	        // 2) run the script  (remove all linebreaks and other no space whitespace-characters
	        String script = prepareScript();
	        String fixedScript = fixEncoding(script);
	        
	        parseScript(connection, fixedScript);
	        
	        if(useEvaluate) {
	        	// parse and run script
	        	// evaluation list, can be used to create a console view, throws first R-error-message
	        	REXPGenericVector knimeEvalObj = evaluateScript(fixedScript, connection);
	        	// check for warnings
	        	ArrayList<String> warningMessages = RUtils.checkForWarnings(connection);
	        	if(warningMessages.size() > 0) setWarningMessage("R-script produced " + warningMessages.size() + " warnings. See R-console view for further details");
	        	
	
	        } else {
	        	// parse and run script
	        	evalScript(connection, fixedScript);     	
	        }

	        // check if result data frame is present
	    	if( ((REXPLogical) connection.eval("exists(\"" + RSnippetNodeModel.R_INVAR_BASE_NAME + "\")")).isFALSE()[0] ) 
	    		throw new KnimeScriptingException("R workspace does not contain " + RSnippetNodeModel.R_INVAR_BASE_NAME + " after execution.");    	
	        REXP out = connection.eval(RSnippetNodeModel.R_INVAR_BASE_NAME);
	        if(!out.inherits("data.frame")) 
	        	throw new KnimeScriptingException(RSnippetNodeModel.R_INVAR_BASE_NAME + " is not a data frame");
	        
	        // extract output data-frame from R
	        dataTable = pullTableFromR(RSnippetNodeModel.R_INVAR_BASE_NAME, connection, exec, chunkOutSize);
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }
        connection.close();

        return new BufferedDataTable[]{dataTable};
	}

	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec) throws KnimeScriptingException {
		super.openInR(inData, exec);
	}


	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec) throws KnimeScriptingException {
		try {
			String rawScript = prepareScript();
			openInR(inData, exec, rawScript, logger);   
			setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
		} catch (REXPMismatchException | IOException | REngineException e) {
			throw new KnimeScriptingException("Failed to open in R\n" + e);
		}	
	}*/
}

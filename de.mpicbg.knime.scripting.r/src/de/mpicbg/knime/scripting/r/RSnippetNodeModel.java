package de.mpicbg.knime.scripting.r;

import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * This is the model implementation of RSnippet.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class RSnippetNodeModel extends AbstractScriptingNodeModel {

    public static final String R_INVAR_BASE_NAME = "kIn";
    public static final String R_OUTVAR_BASE_NAME = "rOut";

    /**
     * constructor
     * @param numInputs
     * @param numOutputs
     */
    public RSnippetNodeModel(int numInputs, int numOutputs) {
        super(createPorts(numInputs), createPorts(numOutputs));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultScript() {
        return RUtils.SCRIPT_PROPERTY_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		// check preferences
    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);

    	// create connection
        RConnection connection = RUtils.createConnection();
        
        // try to execute the node, close connection if anything fails and pass through the error
        BufferedDataTable outTable = null;
        try {
        	outTable = transferAndEvaluate(castToBDT(inData), exec, connection);
        } catch (Exception e) {
			if(connection.isConnected()) {
				connection.close();
				throw e;
			}
		}
        
        return new BufferedDataTable[]{outTable};
    }

    /**
     * execute node
     * @param inData
     * @param exec
     * @param connection
     * @return out table
     * @throws KnimeScriptingException
     * @throws RserveException
     * @throws REXPMismatchException
     * @throws CanceledExecutionException
     */
	private BufferedDataTable transferAndEvaluate(
			final BufferedDataTable[] inData, final ExecutionContext exec,
			RConnection connection)
			throws KnimeScriptingException, RserveException,
			REXPMismatchException, CanceledExecutionException {
		
		// check preferences
    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);
        DataTableSpec inSpec = inData[0].getDataTableSpec();
    	
    	// retrieve chunk settings
        int chunkInSize = RUtils.getChunkIn(((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_IN)).getIntValue(), inData);
        int chunkOutSize = ((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_OUT)).getIntValue();
    	
    	// push color/size/shape model to R
		RUtils.pushColorModelToR(inSpec, connection, exec);
		RUtils.pushShapeModelToR(inSpec, connection, exec);
		RUtils.pushSizeModelToR(inSpec, connection, exec);
		
		// push flow variables to R
		RUtils.pushFlowVariablesToR(getAvailableInputFlowVariables(), connection, exec);

        // CONVERT input table into data-frame and put into the r-workspace
        RUtils.pushToR(inData, connection, exec, chunkInSize);
        
        exec.setMessage("Evaluate R-script (cannot be cancelled)");

        // PREPARE and parse script
        String script = prepareScript();
        // LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
        // stop support !
        //rawScript = RUtils.supportOldVarNames(rawScript);   
        RUtils.parseScript(connection, script);

        // EVALUATE script
        if(useEvaluate) {
        	// parse and run script
        	// evaluation list, can be used to create a console view, throws first R-error-message
        	REXPGenericVector knimeEvalObj = RUtils.evaluateScript(script, connection);
        	// check for warnings
        	ArrayList<String> warningMessages = RUtils.checkForWarnings(connection);
        	if(warningMessages.size() > 0) setWarningMessage("R-script produced " + warningMessages.size() + " warnings. See R-console view for further details");
        	

        } else {
        	// parse and run script
        	RUtils.evalScript(connection, script);     	
        }

        // CHECK if result data frame is present
    	if( ((REXPLogical) connection.eval("exists(\"" + R_OUTVAR_BASE_NAME + "\")")).isFALSE()[0] ) 
    		throw new KnimeScriptingException("R workspace does not contain " + R_OUTVAR_BASE_NAME + " after execution.");    	
        REXP out = connection.eval(R_OUTVAR_BASE_NAME);
        if(!out.inherits("data.frame")) 
        	throw new KnimeScriptingException(R_OUTVAR_BASE_NAME + " is not a data frame");
        
        // EXTRACT output data-frame from R
        BufferedDataTable outTable = RUtils.pullTableFromR(R_OUTVAR_BASE_NAME, connection, exec, chunkOutSize);
		return outTable;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		try {
			String rawScript = prepareScript();
			RUtils.openInR(inData, exec, rawScript, logger);   
			setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
		} catch (REXPMismatchException | IOException | REngineException e) {
			throw new KnimeScriptingException("Failed to open in R\n" + e);
		}		
	}
	


}


package de.mpicbg.knime.scripting.r;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.AbstractTableScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * This is the model implementation of RSnippet. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RSnippetNodeModel extends AbstractTableScriptingNodeModel {

    public static final String R_INVAR_BASE_NAME = "kIn";
    public static final String R_OUTVAR_BASE_NAME = "rOut";


    public RSnippetNodeModel(int numInputs, int numOutputs) {
        super(numInputs, numOutputs);
    }

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
        
        RUtils.pushColorModelToR(inData[0].getDataTableSpec(), connection, exec);

        // 1) convert input table into data-frame and put into the r-workspace
        RUtils.pushToR(inData, connection, exec.createSubProgress(1.0/2));
        
        // TODO: push color/size/shape model to R
        // TODO: push flow variables to R

        // 2) prepare and parse script
        String script = prepareScript();
        // LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
        // stop support !
        //rawScript = RUtils.supportOldVarNames(rawScript);   
        RUtils.parseScript(connection, script);

        // 3) evaluate script
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

        // check if result data frame is present
    	if( ((REXPLogical) connection.eval("exists(\"" + R_OUTVAR_BASE_NAME + "\")")).isFALSE()[0] ) 
    		throw new KnimeScriptingException("R workspace does not contain " + R_OUTVAR_BASE_NAME + " after execution.");    	
        REXP out = connection.eval(R_OUTVAR_BASE_NAME);
        if(!out.inherits("data.frame")) 
        	throw new KnimeScriptingException(R_OUTVAR_BASE_NAME + " is not a data frame");
        
        // 4) extract output data-frame from R
        BufferedDataTable outTable = RUtils.pullTableFromR(R_OUTVAR_BASE_NAME, connection, exec);
        
        return new BufferedDataTable[]{outTable};
    }

    private static Map<String, DataType> getColumnTypeMapping(BufferedDataTable bufferedDataTable) {
        Iterator<DataColumnSpec> dataColumnSpecIterator = bufferedDataTable.getSpec().iterator();
        Map<String, DataType> typeMapping = new HashMap<String, DataType>();
        while (dataColumnSpecIterator.hasNext()) {
            DataColumnSpec dataColumnSpec = dataColumnSpecIterator.next();
            typeMapping.put(dataColumnSpec.getName(), dataColumnSpec.getType());
        }

        return typeMapping;
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


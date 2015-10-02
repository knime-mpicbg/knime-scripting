package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.AbstractTableScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


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


    @Override
    protected BufferedDataTable[] executeImpl(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {
    	
    	// check preferences
    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);

        RConnection connection = RUtils.createConnection();
        
        BufferedDataTable dataTable = null;

        try {
	        // 1) convert exampleSet into data-frame and put into the r-workspace
	        RUtils.pushToR(inData, connection, exec);
	
	        // 2) run the script  (remove all line breaks and other no space whitespace-characters
	//        connection.eval(RUtils.prepare4RExecution(script.getStringValue()));
	
	        String rawScript = prepareScript();
	
	        // LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
	        rawScript = RUtils.supportOldVarNames(rawScript);
	
	        String fixedScript = RUtils.fixEncoding(rawScript);
	        
	        REXP out = null;
	        String[] rowNames = null;
	        
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
	    	if( ((REXPLogical) connection.eval("exists(\"" + R_OUTVAR_BASE_NAME + "\")")).isFALSE()[0] ) 
	    		throw new KnimeScriptingException("R workspace does not contain " + R_OUTVAR_BASE_NAME + " after execution.");
	    	
	    	out = connection.eval(R_OUTVAR_BASE_NAME);
	        if(!out.inherits("data.frame")) 
	        	throw new KnimeScriptingException(R_OUTVAR_BASE_NAME + " is not a data frame");
	        
	        // retrieve row names
	        rowNames = connection.eval("rownames(" + R_OUTVAR_BASE_NAME + ")").asStrings();
	
	        Map<String, DataType> typeMapping = getColumnTypeMapping(inData[0]);
	
	        // 3) extract output data-frame from R
	        assert(out != null);
	        dataTable = RUtils.convert2DataTable(exec, out, rowNames, typeMapping);
	
	        connection.eval("rm(list = ls(all = TRUE));");
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }
        connection.close();

        return new BufferedDataTable[]{dataTable};
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


    public String getDefaultScript() {
        return RUtils.SCRIPT_PROPERTY_DEFAULT;
    }


	@Override
	protected void openIn(BufferedDataTable[] inData, ExecutionContext exec)
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


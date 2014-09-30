package de.mpicbg.knime.scripting.r.generic;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.Rserve.RConnection;

import java.io.File;
import java.util.ArrayList;


/**
 * A generic R node which transforms generic R objects and not just BufferedDataTables (aka. data frames)
 *
 * @author Holger Brandl
 */
public class GenericRSnippet extends AbstractScriptingNodeModel {

    private File rWorkspaceFile;


    public GenericRSnippet() {
        // the input port is optional just to allow generative R nodes
        this(createPorts(1, RPortObject.TYPE, RPortObject.class), createPorts(1, RPortObject.TYPE, RPortObject.class));
    }


    protected GenericRSnippet(PortType[] inPortTypes, PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }


    @Override
    protected PortObject[] execute(PortObject[] inData, ExecutionContext exec) throws Exception {
    	
    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);
    	
        RConnection connection = RUtils.createConnection();

        // 1) restore the workspace in a different server session
        RUtils.pushToR(inData, connection, exec);


        // 2) run the script  (remove all linebreaks and other no space whitespace-characters
        String script = prepareScript();
        String fixedScript = RUtils.fixEncoding(script);
        
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


        // 3) extract output data-frame from R
        if (rWorkspaceFile == null) {
            rWorkspaceFile = File.createTempFile("genericR", RSnippetNodeModel.R_OUTVAR_BASE_NAME);
        }

        RUtils.saveToLocalFile(rWorkspaceFile, connection, RUtils.getHost(), RSnippetNodeModel.R_OUTVAR_BASE_NAME);

        connection.close();

        return new PortObject[]{new RPortObject(rWorkspaceFile)};
    }


    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
//        checkRExecutable();
        return hasOutput() ? new PortObjectSpec[]{RPortObjectSpec.INSTANCE} : new PortObjectSpec[0];
    }


    public String getDefaultScript() {
        return RUtils.SCRIPT_PROPERTY_DEFAULT;
    }
}

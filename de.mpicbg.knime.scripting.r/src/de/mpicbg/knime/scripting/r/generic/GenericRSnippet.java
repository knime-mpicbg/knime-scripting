package de.mpicbg.knime.scripting.r.generic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.REXPGenericVector;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * A generic R node which transforms generic R objects and not just BufferedDataTables (aka. data frames)
 *
 * @author Holger Brandl
 * {@deprecated}
 */
public class GenericRSnippet extends AbstractRScriptingNodeModel {

    private File rWorkspaceFile;


    public GenericRSnippet() {
        // the input port is optional just to allow generative R nodes
        this(createPorts(1, RPortObject.TYPE, RPortObject.class), createPorts(1, RPortObject.TYPE, RPortObject.class));
    }


    protected GenericRSnippet(PortType[] inPortTypes, PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes, new RColumnSupport());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	super.configure(inSpecs);
        return hasOutput() ? new PortObjectSpec[]{RPortObjectSpec.INSTANCE} : new PortObjectSpec[0];
    }


    public String getDefaultScript() {
        return AbstractRScriptingNodeModel.CFG_SCRIPT_DFT;
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);

        try {
	        // 1) restore the workspace in a different server session
	        //pushToR(inData, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);
        	m_con = RUtils.createConnection();
        	RUtils.loadGenericInputs(Collections.singletonMap(RSnippetNodeModel.R_INVAR_BASE_NAME, ((RPortObject)inData[0]).getFile()), m_con);
	
	        // 2) run the script  (remove all linebreaks and other no space whitespace-characters
	        String script = prepareScript();
	        String fixedScript = fixEncoding(script);
	        
	        parseScript(m_con, fixedScript);
	        
	        if(useEvaluate) {
	        	// parse and run script
	        	// evaluation list, can be used to create a console view, throws first R-error-message
	        	REXPGenericVector knimeEvalObj = evaluateScript(fixedScript, m_con);
	        	// check for warnings
	        	ArrayList<String> warningMessages = RUtils.checkForWarnings(m_con);
	        	if(warningMessages.size() > 0) setWarningMessage("R-script produced " + warningMessages.size() + " warnings. See R-console view for further details");
	        	
	
	        } else {
	        	// parse and run script
	        	evalScript(m_con, fixedScript);     	
	        }
	
	
	        // 3) extract output data-frame from R
	        if (rWorkspaceFile == null) {
	            rWorkspaceFile = File.createTempFile("genericR", RSnippetNodeModel.R_OUTVAR_BASE_NAME);
	        }
	
	        RUtils.saveToLocalFile(rWorkspaceFile, m_con, RUtils.getHost(), RSnippetNodeModel.R_OUTVAR_BASE_NAME);
        } catch(Exception e) {
        	closeRConnection();
        	throw e;
        }

        closeRConnection();

        return new PortObject[]{new RPortObject(rWorkspaceFile)};
	}

    /**
     * {@inheritDoc}
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		/*try {
			String rawScript = prepareScript();
			openInR(inData, exec, rawScript, logger);   
			setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
		} catch (REXPMismatchException | IOException | REngineException e) {
			throw new KnimeScriptingException("Failed to open in R\n" + e);
		}*/
		
	}
}

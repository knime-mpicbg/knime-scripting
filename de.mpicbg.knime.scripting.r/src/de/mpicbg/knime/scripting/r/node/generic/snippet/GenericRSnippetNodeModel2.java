package de.mpicbg.knime.scripting.r.node.generic.snippet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.port.RPortObjectSpec;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * Model: A generic R node which modifies an R workspace
 *
 * @author Holger Brandl, Antje Janosch
 */
public class GenericRSnippetNodeModel2 extends AbstractRScriptingNodeModel {
	
	/** default R script */
	public static final String GENERIC_SNIPPET_DFT = 
			"# put your R code here\n"
			+ "# availableObjects <- ls()";
	
	public static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, RPortObject.TYPE, RPortObject.class), 	// 1 generic input
			createPorts(1, RPortObject.TYPE, RPortObject.class)	, 	// 1 generic output
			new RColumnSupport(), 		
			true, 	// script
			true, 	// open in R functionality
			false);	// no chunks
			
	/**
	 * constructor for 1 RPort input and 1 RPort output
	 */
    public GenericRSnippetNodeModel2(ScriptingModelConfig cfg) {
        super(cfg);
    }

    /**
     * constructor
     * @param inPortTypes
     * @param outPortTypes
     * @param useChunkSettings 
     * @param useOpenIn 
     */
    public GenericRSnippetNodeModel2() {
        this(nodeModelConfig);
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
    	return GENERIC_SNIPPET_DFT;
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		
    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);
    	
    	RConnection connection = RUtils.createConnection();
    	File rWorkspaceFile = null;
    	RPortObject outPort = null;

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
	
	
	     // 2) write a local workspace file which contains the input table of the node
        	if (rWorkspaceFile == null) {
        		rWorkspaceFile = File.createTempFile("genericR", ".RData");  //Note: this r is just a filename suffix
        	}
        	RUtils.saveWorkspaceToFile(rWorkspaceFile, connection, RUtils.getHost());
        	
        	outPort = new RPortObject(connection, rWorkspaceFile);
        	exec.setProgress(1.0);
        	
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }

        connection.close();

        return new PortObject[]{outPort};
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

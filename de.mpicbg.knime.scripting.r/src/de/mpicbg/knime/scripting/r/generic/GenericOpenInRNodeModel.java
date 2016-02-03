package de.mpicbg.knime.scripting.r.generic;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * This is the model implementation of generic version of OpenInR. It allows to spawn a new instance of R using the node
 * input as workspace initialization. It's main purpose is for prototyping.
 *
 * @author Holger Brandl (MPI-CBG)
 * @deprecated
 */
public class GenericOpenInRNodeModel extends AbstractRScriptingNodeModel {

	/* (non-Javadoc)
	 * @see de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel#hasOutput()
	 */
	@Override
	protected boolean hasOutput() {
		// TODO Auto-generated method stub
		return false;
	}
	
    @Override
	/* (non-Javadoc)
	 * @see org.knime.core.node.NodeModel.configure(final PortObjectSpec[] inSpecs)
	 */
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[0];
    }

    /**
     * @deprecated
     */
	protected GenericOpenInRNodeModel() {
        super(createPorts(1, RPortObject.TYPE, RPortObject.class), new PortType[0], new RColumnSupport());
    }
   
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		//create connection to server
    	logger.info("Creating R-connection");
    	m_con = RUtils.createConnection();
        
        File rWorkspaceFile = null;
        try {
        // push incoming data to R server
        logger.info("Pushing inputs to R...");
        RUtils.loadGenericInputs(Collections.singletonMap(RSnippetNodeModel.R_INVAR_BASE_NAME, ((RPortObject)inData[0]).getFile()), m_con);
        
        // save workspace file and return it to local machine
        rWorkspaceFile = File.createTempFile("genericR", RSnippetNodeModel.R_INVAR_BASE_NAME);
        RUtils.saveToLocalFile(rWorkspaceFile, m_con, RUtils.getHost(), RSnippetNodeModel.R_INVAR_BASE_NAME);
        } catch(Exception e) {
        	closeRConnection();
        	throw e;
        }
        
        // close connection to server
        closeRConnection();
        
        // open R with workspace file
        logger.info("Spawning R-instance ...");
        openWSFileInR(rWorkspaceFile, prepareScript());

        return new PortObject[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		try {
			executeImpl(inData, exec);
		} catch (Exception e) {
			throw new KnimeScriptingException(e.getMessage());
		}
	}

	public static void openWSFileInR(File workspaceFile, String script) throws IOException {
	    IPreferenceStore prefStore = R4KnimeBundleActivator.getDefault().getPreferenceStore();
	    String rExecutable = prefStore.getString(RPreferenceInitializer.LOCAL_R_PATH);
	
	    // 3) spawn a new R process
	    if (Utils.isWindowsPlatform()) {
	        Runtime.getRuntime().exec(rExecutable + " " + workspaceFile.getAbsolutePath());
	    } else if (Utils.isMacOSPlatform()) {
	
	        Runtime.getRuntime().exec("open -n -a " + rExecutable + " " + workspaceFile.getAbsolutePath());
	
	    } else { // linux and the rest of the world
	        Runtime.getRuntime().exec(rExecutable + " " + workspaceFile.getAbsolutePath());
	    }
	
	    // copy the script in the clipboard
	    if (!script.isEmpty()) {
	        StringSelection data = new StringSelection(script);
	        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	        clipboard.setContents(data, data);
	    }
	}
}
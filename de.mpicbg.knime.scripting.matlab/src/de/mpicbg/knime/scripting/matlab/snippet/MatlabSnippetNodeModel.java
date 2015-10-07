package de.mpicbg.knime.scripting.matlab.snippet;

import java.io.ByteArrayInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabCode;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabFileTransfer;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabTable;


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
        return AbstractMatlabScriptingNodeModel.DEFAULT_SNIPPET;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
    	BufferedDataTable outData = null;
    	
    	try {
//    		this.initializeMatlabClient();
    		
            // Get preference pane properties
            this.matlabWorkspaceType = preferences.getString(MatlabPreferenceInitializer.MATLAB_TYPE);
            this.tableTransferMethod = preferences.getString(MatlabPreferenceInitializer.MATLAB_TRANSFER_METHOD);
            
            // Get the code
    		String snippet = prepareScript();
    		exec.checkCanceled();
    		
    		// Execute it
    		// Prepare snippet temp-file
			codeFile = new MatlabFileTransfer(AbstractMatlabScriptingNodeModel.SNIPPET_TEMP_FILE_PREFIX, 
					AbstractMatlabScriptingNodeModel.SNIPPET_TEMP_FILE_SUFFIX);
			
			table = new MatlabTable((BufferedDataTable)inData[0]);
			
			if (tableTransferMethod.equals("file")) {
				// Convert the KNIME table and write it to the temp-directory
				table.writeHashMapToTempFolder();
				
				// Prepare the MATLAB parser script
		        parserFile = new MatlabFileTransfer(AbstractMatlabScriptingNodeModel.MATLAB_HASHMAP_SCRIPT);

				// Add the MATLAB code to the snippet and transfer the scripts to the temp-directory
				code = new MatlabCode(snippet, matlabWorkspaceType, 
						parserFile.getPath(), 
						codeFile.getPath(), 
						table.getHashMapTempPath());
				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
		        String cmd = code.getScriptExecutionCommand(codeFile.getPath(), false, true);		

				// Run it in MATLAB
		        matlabProxy = matlabConnector.acquireProxyFromQueue();
		        matlabProxy.eval(cmd);
				MatlabCode.checkForScriptErrors(matlabProxy);
				matlabProxy.eval(MatlabCode.getSnippetNodeMessage(false));
//				releaseMatlabProxy(proxy);

				// Get the data back
				table.readHashMapFromTempFolder(exec);
				outData = table.getBufferedDataTable();
				
			} else if (tableTransferMethod.equals("workspace")) {
				// Create a script from the snippet
				code = new MatlabCode(snippet, matlabWorkspaceType, 
						codeFile.getPath());
				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getPath(), true, true);
				
				// Get a proxy (block it) push the data execute the snippet and pull back the modified data
				matlabProxy = matlabConnector.acquireProxyFromQueue();
				table.pushTable2MatlabWorkspace(matlabProxy, matlabWorkspaceType);
				matlabProxy.eval(cmd);
				MatlabCode.checkForScriptErrors(matlabProxy);
				matlabProxy.eval(MatlabCode.getSnippetNodeMessage(true)); //TODO pack this in a function in matlabCode.
				outData = table.pullTableFromMatlabWorkspace(exec, matlabProxy, matlabWorkspaceType);
//				releaseMatlabProxy(proxy);

				
			}
			
//    		BufferedDataTable table = this.matlabConnector.client.snippetTask(inData[0], this.tableTransferMethod, exec, snippet, this.matlabWorkspaceType);
//    		outData[0] = table;
    		exec.checkCanceled();
    		
    		// Housekeeping
    		cleanup();
//    		exec.checkCanceled();

    	} catch (Exception e) {
    		throw e;
    	} finally {
    		if ((matlabConnector != null) && (matlabProxy != null))
    			matlabConnector.returnProxyToQueue(matlabProxy);
    	}
    	
    	return new BufferedDataTable[]{outData};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		throw new KnimeScriptingException("The functionality to open data external is not yet implemented");
	}

}

package de.mpicbg.knime.scripting.r.generic;

import java.io.File;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.OpenInRNodeModel;
import de.mpicbg.knime.scripting.r.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;


/**
 * This is the model implementation of generic version of OpenInR. It allows to spawn a new instance of R using the node
 * input as workspace initialization. It's main purpose is for prototyping.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class GenericOpenInRNodeModel extends AbstractScriptingNodeModel {

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

	protected GenericOpenInRNodeModel() {
        super(createPorts(1, RPortObject.TYPE, RPortObject.class), new PortType[0]);
    }
   
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		//create connection to server
    	logger.info("Creating R-connection");
        RConnection connection = RUtils.createConnection();
        
        File rWorkspaceFile = null;
        try {
        // push incoming data to R server
        logger.info("Pushing inputs to R...");
        RUtils.pushToR(inData, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);
        
        // save workspace file and return it to local machine
        rWorkspaceFile = File.createTempFile("genericR", RSnippetNodeModel.R_INVAR_BASE_NAME);
        RUtils.saveToLocalFile(rWorkspaceFile, connection, RUtils.getHost(), RSnippetNodeModel.R_INVAR_BASE_NAME);
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }
        
        // close connection to server
        connection.close();
        
        // open R with workspace file
        logger.info("Spawning R-instance ...");
        OpenInRNodeModel.openWSFileInR(rWorkspaceFile, prepareScript());

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
}
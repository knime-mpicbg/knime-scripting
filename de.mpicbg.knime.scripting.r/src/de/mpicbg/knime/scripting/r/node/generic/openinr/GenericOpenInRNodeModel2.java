package de.mpicbg.knime.scripting.r.node.generic.openinr;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.openinr.OpenInRNodeModel2;
import de.mpicbg.knime.scripting.r.port.RPortObject;


/**
 * This is the model implementation of generic version of OpenInR. It allows to spawn a new instance of R using the node
 * input as workspace initialization. It's main purpose is for prototyping.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class GenericOpenInRNodeModel2 extends AbstractScriptingNodeModel {
	
	/**
	 * {@inheritDoc}
	 */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[0];
    }

    /**
     * constructor: 1 R Port input, no output
     */
	protected GenericOpenInRNodeModel2() {
        super(createPorts(1, RPortObject.TYPE, RPortObject.class), new PortType[0], 
        		new RColumnSupport(), false, false, false);
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
        	
        	// write a local workspace file which contains the input table of the node
        	if (rWorkspaceFile == null) {
        		rWorkspaceFile = File.createTempFile("genericR", ".RData");  
        	}
        	RUtils.saveWorkspaceToFile(rWorkspaceFile, connection, RUtils.getHost());
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }

        // close connection to server
        connection.close();

        // open R with workspace file
        exec.setMessage("Spawning R-instance ...");
        try {
			OpenInRNodeModel2.openWSFileInR(rWorkspaceFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new KnimeScriptingException("Spawning of R-process failed: " + e.getMessage());
		}
        exec.setProgress(1.0);

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
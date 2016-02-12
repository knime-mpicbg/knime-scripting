package de.mpicbg.knime.scripting.r.generic;

import java.io.File;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;



/**
 * A generic R node which transforms generic R objects and not just BufferedDataTables (aka. data frames)
 *
 * @author Holger Brandl
 */
public class ConvertToGenericR extends AbstractRScriptingNodeModel {
    
	public static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
    		createPorts(1), 
    		createPorts(1, RPortObject.TYPE, RPortObject.class), 
    		new RColumnSupport(), 
    		false, 
    		true, 
    		true);


    public ConvertToGenericR() {
        // the input port is optional just to allow generative R nodes
        super(nodeModelConfig);
    }

	@Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{RPortObjectSpec.INSTANCE};
    }


    @Override
    protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
    	super.executeImpl(inData, exec);
    	File rWorkspaceFile = null;

    	try{
    		String script = R_OUTVAR_BASE_NAME + " <- " + R_INVAR_BASE_NAME;

    		try {
    			evalScript(m_con, script);     	
    		} catch (RserveException | REXPMismatchException | KnimeScriptingException e) {
    			closeRConnection();
    			throw new KnimeScriptingException("Failed to evaluate the script:\n" + e.getMessage());
    		}

    		// 2) write a local workspace file which contains the input table of the node
    		if (rWorkspaceFile == null) {
    			rWorkspaceFile = File.createTempFile("genericR", "R");  //Note: this r is just a filename suffix
    		}

    		RUtils.saveToLocalFile(rWorkspaceFile, m_con, RUtils.getHost(), R_OUTVAR_BASE_NAME);
    	} catch(Exception e) {
    		closeRConnection();
    		throw e;
    	}
    	closeRConnection();

    	return new PortObject[]{new RPortObject(rWorkspaceFile)};
    }
}

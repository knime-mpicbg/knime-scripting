package de.mpicbg.knime.scripting.python.v2.node.snippet22;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;

/**
 * Node Model for Python Snippet node
 * 
 * @author Antje Janosch
 *
 */
public class PythonSnippet22V2NodeModel extends AbstractPythonScriptingV2NodeModel {
    
	// node architecture
    public static final ScriptingModelConfig nodeModelCfg = new ScriptingModelConfig(
    			createPorts(2), 		// 1 input table
    			createPorts(2), 		// 1 output table
    			new PythonColumnSupport(), 	
    			true, 					// script
    			true,					// provide openIn
    			false);					// use chunks
    
    private static final String DFT_SCRIPT = "pyOut1 = kIn1.copy()\npyOut2 = kIn2.copy";

    /**
     * constructor with node architecture configurations
     * @param cfg	{@link ScriptingModelConfig}
     */
    public PythonSnippet22V2NodeModel(ScriptingModelConfig cfg) {
        super(cfg);
    }

    /**
     * constructor
     */
	public PythonSnippet22V2NodeModel() {
		super(nodeModelCfg);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultScript(String defaultScript) {
    	return DFT_SCRIPT;
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		
		super.pushInputToPython(inData, exec);
		super.runScript(exec);
		PortObject[] outData = super.pullOutputFromPython(exec);
   
        return outData;
    }
	
}


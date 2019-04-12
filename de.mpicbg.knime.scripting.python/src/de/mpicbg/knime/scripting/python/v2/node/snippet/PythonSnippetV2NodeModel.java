package de.mpicbg.knime.scripting.python.v2.node.snippet;

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
public class PythonSnippetV2NodeModel extends AbstractPythonScriptingV2NodeModel {
    
	// node architecture
    public static final ScriptingModelConfig nodeModelCfg = new ScriptingModelConfig(
    			createPorts(1), 		// 1 input table
    			createPorts(1), 		// 1 output table
    			new PythonColumnSupport(), 	
    			true, 					// script
    			true,					// provide openIn
    			false);					// use chunks

    /**
     * constructor with node architecture configurations
     * @param cfg	{@link ScriptingModelConfig}
     */
    public PythonSnippetV2NodeModel(ScriptingModelConfig cfg) {
        super(cfg);
    }

    /**
     * constructor
     */
	public PythonSnippetV2NodeModel() {
		super(nodeModelCfg);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultScript(String defaultScript) {
    	return super.getDefaultScript(CFG_SCRIPT_DFT);
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


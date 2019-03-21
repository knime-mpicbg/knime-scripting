package de.mpicbg.knime.scripting.python.v2.node.snippet;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;


public class PythonSnippetV2NodeModel extends AbstractPythonScriptingV2NodeModel {
    
    private static final ScriptingModelConfig nodeModelCfg = new ScriptingModelConfig(
    			createPorts(1), 		// 1 input table
    			createPorts(1), 		// 1 output table
    			new PythonColumnSupport(), 	
    			true, 					// script
    			true,					// provide openIn
    			true);					// use chunks

    /**
     * constructor
     * @param numInputs
     * @param numOutputs
     */
    public PythonSnippetV2NodeModel(ScriptingModelConfig cfg) {
        super(cfg);
    }

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
		
		super.executeImpl(inData, exec);
		super.runScript(exec);
		//PortObject[] outData = super.pullOutputFromR(exec);
   
        //return outData;
		return null;
    }
	
}


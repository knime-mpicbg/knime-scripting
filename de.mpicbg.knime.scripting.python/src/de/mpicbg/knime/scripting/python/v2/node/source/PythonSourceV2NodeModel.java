package de.mpicbg.knime.scripting.python.v2.node.source;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;

public class PythonSourceV2NodeModel extends AbstractPythonScriptingV2NodeModel {
	
	// node architecture
    public static final ScriptingModelConfig nodeModelCfg = new ScriptingModelConfig(
    			createPorts(0), 		// 0 input table
    			createPorts(1), 		// 1 output table
    			new PythonColumnSupport(), 	
    			true, 					// script
    			true,					// provide openIn
    			false);					// use chunks
    
    public static final String DFT_SCRIPT = "pyOut1 = pd.DataFrame() ";

    /**
     * constructor with node architecture configurations
     * @param cfg	{@link ScriptingModelConfig}
     */
    public PythonSourceV2NodeModel(ScriptingModelConfig cfg) {
        super(cfg);
    }

    /**
     * constructor
     */
	public PythonSourceV2NodeModel() {
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
		super.prepareScriptFile();
		super.runScript(exec);
		PortObject[] outData = super.pullOutputFromPython(exec);
   
        return outData;
    }
}

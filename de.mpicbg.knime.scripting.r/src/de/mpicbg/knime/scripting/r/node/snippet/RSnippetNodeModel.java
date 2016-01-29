package de.mpicbg.knime.scripting.r.node.snippet;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;


/**
 * This is the model implementation of RSnippet.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class RSnippetNodeModel extends AbstractRScriptingNodeModel {
    
    private static final ScriptingModelConfig nodeModelCfg = new ScriptingModelConfig(
    			createPorts(1), 		// 1 input table
    			createPorts(1), 		// 1 output table
    			new RColumnSupport(), 	
    			true, 					// script
    			true,					// provide openIn
    			true);					// use chunks

    /**
     * constructor
     * @param numInputs
     * @param numOutputs
     */
    public RSnippetNodeModel(ScriptingModelConfig cfg) {
        super(cfg);
    }

	public RSnippetNodeModel() {
		super(nodeModelCfg);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultScript(String defaultScript) {
    	if(this.getNrInPorts() > 1)
    		return super.getDefaultScript(CFG_SCRIPT2_DFT);
    	else
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
		PortObject[] outData = super.pullOutputFromR(exec);
   
        return outData;
    }
	
}


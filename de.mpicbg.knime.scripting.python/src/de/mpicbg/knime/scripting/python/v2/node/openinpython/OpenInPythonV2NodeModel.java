package de.mpicbg.knime.scripting.python.v2.node.openinpython;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;

public class OpenInPythonV2NodeModel extends AbstractPythonScriptingV2NodeModel {


	private static ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(3, 2,3), 	// 3 inputs, input 2 and 3 optional
			createPorts(0), 		// no output
			new PythonColumnSupport(), 	
			false, 					// no script
			false, 					// open in functionality
			false);					// use chunk settings
	
    /**
     * Constructor for the node model.
     */
	public OpenInPythonV2NodeModel() {
		super(nodeModelConfig);
	}
	
    /**
     * {@inheritDoc}
     * @throws KnimeScriptingException 
     * @throws CanceledExecutionException 
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws KnimeScriptingException, CanceledExecutionException  {
		
		super.openIn(inData, exec);
		setWarningMessage("To push the node's input to Python again, you need to reset and re-execute it.");
       
        return new BufferedDataTable[0];
	}
}

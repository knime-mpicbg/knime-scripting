package de.mpicbg.knime.scripting.r.node.openinr;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;


/**
 * This is the model implementation of OpenInR.
 *
 * @author Antje Janosch (MPI-CBG)
 */
public class OpenInRNodeModel2 extends AbstractRScriptingNodeModel {

	private static ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(3, 2,3), 	// 3 inputs, input 2 and 3 optional
			createPorts(0), 		// no output
			new RColumnSupport(), 	
			false, 					// no script
			false, 					// open in functionality
			true);					// use chunk settings

    /**
     * Constructor for the node model.
     */
    protected OpenInRNodeModel2() {
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
		setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
       
        return new BufferedDataTable[0];
	}

}
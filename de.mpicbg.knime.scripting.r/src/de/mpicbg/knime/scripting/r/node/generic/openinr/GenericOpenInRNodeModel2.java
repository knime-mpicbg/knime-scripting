package de.mpicbg.knime.scripting.r.node.generic.openinr;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.port.RPortObject;


/**
 * This is the model implementation of generic version of OpenInR. It allows to spawn a new instance of R using the node
 * input as workspace initialization. It's main purpose is for prototyping.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class GenericOpenInRNodeModel2 extends AbstractRScriptingNodeModel {
	
	private static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, RPortObject.TYPE, RPortObject.class), 	// 1 generic input
			new PortType[0], 										// no output
			new RColumnSupport(), 
			false, 	// no script		
			false,	// no open in R
			false);	// no chunks


    /**
     * constructor: 1 R Port input, no output
     */
	protected GenericOpenInRNodeModel2() {
        super(nodeModelConfig);
    }
	
	/**
	 * {@inheritDoc}
	 */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[0];
    }
    
    /**
     * {@inheritDoc}   
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		super.executeImpl(inData, exec);
		openInR(inData, exec);
		return new PortObject[0];
	}

}
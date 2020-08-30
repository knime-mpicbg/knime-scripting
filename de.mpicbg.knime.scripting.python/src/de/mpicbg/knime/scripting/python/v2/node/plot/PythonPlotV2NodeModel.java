package de.mpicbg.knime.scripting.python.v2.node.plot;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.image.ImagePortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.v2.plots.AbstractPythonPlotV2NodeModel;

/**
 * 
 * Node Model class for 'Python Plot' node
 * 
 * @author Antje Janosch
 *
 */
public class PythonPlotV2NodeModel extends AbstractPythonPlotV2NodeModel{

	/** node model configuration */
	private static ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1), 	// 1 input
			createPorts(1, ImagePortObject.TYPE, ImagePortObject.class), 		// no output
			new PythonColumnSupport(), 	
			true, 					// use script
			true, 					// open in functionality
			true);					// use chunk settings

	/**
	 * constructor
	 */
    public PythonPlotV2NodeModel() {
        super(nodeModelConfig);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        super.configure(inSpecs);
        return new PortObjectSpec[]{IM_PORT_SPEC};
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		super.executeImpl(inData, exec);
		
		super.prepareScriptFile();
		super.runScript(exec);
		PortObject[] outData = super.pullOutputFromPython(exec);
		
		return outData;
	}

}

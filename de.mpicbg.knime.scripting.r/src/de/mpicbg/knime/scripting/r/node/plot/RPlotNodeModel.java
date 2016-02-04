package de.mpicbg.knime.scripting.r.node.plot;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.image.ImagePortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;


/**
 * This is the model implementation of a plot panel that requires a data-table and a script as input.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class RPlotNodeModel extends AbstractRPlotNodeModel {
	
	private static ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1), 	// 3 inputs, input 2 and 3 optional
			createPorts(1, ImagePortObject.TYPE, ImagePortObject.class), 		// no output
			new RColumnSupport(), 	
			true, 					// use script
			true, 					// open in functionality
			true);					// use chunk settings

    public RPlotNodeModel() {
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
		PortObject[] outData = super.pullOutputFromR(exec);
		
		return outData;
	}
}
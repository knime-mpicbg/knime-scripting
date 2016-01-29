package de.mpicbg.knime.scripting.r.node.generic.plot;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.image.ImagePortObject;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;
import de.mpicbg.knime.scripting.r.port.RPortObject;


/**
 * A generic R node which creates a figure from a genericR input.
 *
 * @author Holger Brandl, Antje Janosch
 */
public class GenericRPlotNodeModel2 extends AbstractRPlotNodeModel {
	
	/** node configuration */
	private static ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, RPortObject.TYPE, RPortObject.class), 	// 1 generic input
			createPorts(1, ImagePortObject.TYPE, ImagePortObject.class), 		// 1 image output
			new RColumnSupport(), 	
			true, 					// no script
			true, 					// open in functionality
			false);					// use chunk settings

	/**
	 * constructor
	 */
    public GenericRPlotNodeModel2() {
        super(nodeModelConfig);
    }


    /**
     * This constructor is just necessary to allow superclasses to create plot nodes with ouputs
     */
    public GenericRPlotNodeModel2(ScriptingModelConfig cfg) {
        super(cfg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	super.configure(inSpecs);
        return new PortObjectSpec[0];
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		super.executeImpl(inData, exec);
		PortObject[] outData = super.pullOutputFromR(exec);
		
		return outData;
	}
}

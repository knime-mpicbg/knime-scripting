package de.mpicbg.knime.scripting.r.node.generic.converttogeneric;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.port.RPortObjectSpec;


/**
 * A generic R node which transforms KNIME tables to generic R objects
 *
 * @author Antje Janosch
 */
public class ConvertToGenericRModel2 extends AbstractRScriptingNodeModel {
	
	private static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class), // 1 data table input
			createPorts(1, RPortObject.TYPE, RPortObject.class), 			// 1 generic output
			new RColumnSupport(), 
			false, 	// no script
			false, 	// no open in R
			true);	// use chunk settings

	/**
	 * constructor
	 */
    public ConvertToGenericRModel2() {
        super(nodeModelConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{RPortObjectSpec.INSTANCE};
    }

	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		super.executeImpl(inData, exec);
		return pullOutputFromR(exec);
	}


}

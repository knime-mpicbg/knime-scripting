package de.mpicbg.knime.scripting.r.node.generic.converttotable;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.port.RPortObject2;


/**
 * Model: A generic R node which pulls a an R data frame and returns it as KNIME table
 *
 * @author Holger Brandl, Antje Janosch
 */
public class ConvertToTableNodeModel2 extends AbstractRScriptingNodeModel {
	
	public static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, RPortObject2.TYPE, RPortObject2.class),				// 1 generic input
			createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class),	// 1 KNIME table output
			new RColumnSupport(), 
			true, 		// use script
			true, 		// provide open in R
			true);		// use chunks
	
	public static final String GENERIC_TOTABLE_DFT = "rOut <- iris";

	/**
	 * constructor
	 */
    public ConvertToTableNodeModel2() {
        super(nodeModelConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	super.configure(inSpecs);
    	return new PortObjectSpec[]{(DataTableSpec) null};
    }

	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		super.executeImpl(inData, exec);
		runScript(exec);
		return pullOutputFromR(exec);
	}

	@Override
	public String getDefaultScript(String defaultScString) {
		return super.getDefaultScript(GENERIC_TOTABLE_DFT);
	}
}

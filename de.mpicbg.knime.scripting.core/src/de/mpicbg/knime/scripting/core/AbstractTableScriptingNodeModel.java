package de.mpicbg.knime.scripting.core;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;


/**
 * An abstract base class for scripting snippets
 * The class has been used to handle settings defining the output structure of the table (output tab)
 * As it was not used at all, the feature has been removed, so this class does nothing special at the moment.
 * Might be removed later.
 *
 * @author Holger Brandl
 */
public abstract class AbstractTableScriptingNodeModel extends AbstractScriptingNodeModel {

	protected AbstractTableScriptingNodeModel(boolean openInNode, int numInPorts, int numOutPorts, int... optionalInputs) {
		super(openInNode, createPorts(numInPorts, optionalInputs), createPorts(numOutPorts));
	}

    protected AbstractTableScriptingNodeModel(int numInPorts, int numOutPorts, int... optionalInputs) {
        this(createPorts(numInPorts, optionalInputs), createPorts(numOutPorts));
    }


    protected AbstractTableScriptingNodeModel(PortType[] inPortTypes, PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);

    }


    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }


    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }


    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
    }


    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        super.configure(inSpecs);
        return null;
    }






}

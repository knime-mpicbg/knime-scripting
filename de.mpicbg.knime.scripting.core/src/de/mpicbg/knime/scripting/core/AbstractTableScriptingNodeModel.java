package de.mpicbg.knime.scripting.core;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;


/**
 * An abstract base class for all the r-nodes
 *
 * @author Holger Brandl
 */
public abstract class AbstractTableScriptingNodeModel extends AbstractScriptingNodeModel {


    public static final String APPEND_COLS = "append_columns";
    public static final String COLUMN_NAMES = "new_column_names";
    public static final String COLUMN_TYPES = "new_column_types";

    protected final NodeLogger logger = NodeLogger.getLogger(this.getClass());

    protected boolean appendCols = true;
    protected String[] columnNames;
    protected String[] columnTypes;


    protected AbstractTableScriptingNodeModel(int numInPorts, int numOutPorts, int... optionalInputs) {
        this(createPorts(numInPorts, optionalInputs), createPorts(numOutPorts));
    }


    protected AbstractTableScriptingNodeModel(PortType[] inPortTypes, PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);

    }


    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);


        if (hasOutput()) {
            settings.addBoolean(AbstractTableScriptingNodeModel.APPEND_COLS, appendCols);

            settings.addStringArray(AbstractTableScriptingNodeModel.COLUMN_NAMES, columnNames);
            settings.addStringArray(AbstractTableScriptingNodeModel.COLUMN_TYPES, columnTypes);
        }
    }


    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);

        // It can be safely assumed that the settings are valided by the
        // method below.


        if (hasOutput()) {
            appendCols = settings.getBoolean(AbstractTableScriptingNodeModel.APPEND_COLS, false);

            columnNames = settings.getStringArray(AbstractTableScriptingNodeModel.COLUMN_NAMES);
            columnTypes = settings.getStringArray(AbstractTableScriptingNodeModel.COLUMN_TYPES);
        }
    }


    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);

        if (hasOutput()) {
            settings.getBoolean(AbstractTableScriptingNodeModel.APPEND_COLS);

            settings.getStringArray(AbstractTableScriptingNodeModel.COLUMN_NAMES);
            settings.getStringArray(AbstractTableScriptingNodeModel.COLUMN_TYPES);
        }
    }


    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        super.configure(inSpecs);

        return configureInternal(inSpecs);
    }


    /**
     * If hardwired scripting nodes want to provide an own configure, they should rather overried this method.
     */
    protected DataTableSpec[] configureInternal(DataTableSpec[] inSpec) throws InvalidSettingsException {
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        //	append the property columns to the data table spec
        DataTableSpec newSpec = appendCols && getNrInPorts() > 0 ? inSpec[0] : new DataTableSpec();

        if ((columnNames == null || columnNames.length == 0) && !appendCols) {
            return null;
//            return createSpecArray(newSpec);
        }

        for (int i = 0; columnNames != null && i < columnNames.length; i++) {
            DataType type = StringCell.TYPE;
            String columnType = columnTypes[i];

            if ("String".equals(columnType)) {
                type = StringCell.TYPE;
            } else if ("Integer".equals(columnType)) {
                type = IntCell.TYPE;
            } else if ("Double".equals(columnType)) {
                type = DoubleCell.TYPE;
            }
            DataColumnSpec newColumn =
                    new DataColumnSpecCreator(columnNames[i], type).createSpec();

            newSpec = AppendedColumnTable.getTableSpec(newSpec, newColumn);
        }

        return createSpecArray(newSpec);
    }


    private DataTableSpec[] createSpecArray(DataTableSpec newSpec) {
        switch (numOutputs) {
            case 0:
                return new DataTableSpec[0];
            case 1:
                return new DataTableSpec[]{newSpec};

            case 2:
                return new DataTableSpec[]{newSpec, newSpec};

            default:
                throw new RuntimeException("Unsupported number of outputs: " + numOutputs);
        }
    }
}

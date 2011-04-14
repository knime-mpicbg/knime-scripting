package de.mpicbg.tds.knime.knutils;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModel;

import java.util.ArrayList;
import java.util.List;


/**
 * An abstract implementation of the settings pane which keeps a reference to the table-specs.
 *
 * @author Holger Brandl
 */
public abstract class AbstractConfigDialog extends DefaultNodeSettingsPane {

    DataTableSpec tableSpecs;


    List<SettingsModel> swingUISettings = new ArrayList<SettingsModel>();


    /**
     * New pane for configuring a node dialog. This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    public AbstractConfigDialog() {
        createControls();
    }


    /**
     * Another constructor which does not call create-controls. This needs to be done in the extending class itself. By
     * doing so the extending class can initialize internal fields necessary to create the controls.
     */
    public AbstractConfigDialog(Object o) {
    }


    protected abstract void createControls();


    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);

        if (specs.length > 0)
            tableSpecs = specs[0];

        for (SettingsModel swingUISetting : swingUISettings) {
            try {
                swingUISetting.loadSettingsFrom(settings);
            } catch (InvalidSettingsException e) {
                throw new RuntimeException("Problem while loading settings of " + swingUISetting);
            }
        }
    }


    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);

        for (SettingsModel swingUISetting : swingUISettings) {
            swingUISetting.saveSettingsTo(settings);
        }
    }


    public DataTableSpec getSpecs() {
        return tableSpecs;
    }


    public void setTableSpecs(DataTableSpec tableSpecs) {
        this.tableSpecs = tableSpecs;
    }


    protected void registerProperty(SettingsModel settingsModel) {
        swingUISettings.add(settingsModel);
    }
}

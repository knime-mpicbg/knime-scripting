package de.mpicbg.knime.knutils;

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

    DataTableSpec[] tableSpecs;


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
    protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);

        updateSpecs(specs);
        updateSettings(settings);
    }

    private void updateSettings(NodeSettingsRO settings) {

        for (SettingsModel swingUISetting : swingUISettings) {
            try {
                swingUISetting.loadSettingsFrom(settings);
            } catch (InvalidSettingsException e) {
                throw new RuntimeException("Problem while loading settings of " + swingUISetting);
            }
        }
    }

    private void updateSpecs(DataTableSpec[] specs) {
        tableSpecs = specs;
    }

    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);

        updateSpecs(specs);
        updateSettings(settings);
    }


    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);

        for (SettingsModel swingUISetting : swingUISettings) {
            swingUISetting.saveSettingsTo(settings);
        }
    }


    public DataTableSpec getFirstSpec() {
        if (tableSpecs == null) return null;
        if (tableSpecs.length < 1) return null;
        return tableSpecs[0];
    }


    public void setFirstTableSpecs(DataTableSpec tableSpecs) {
        if (this.tableSpecs == null) this.tableSpecs = new DataTableSpec[]{tableSpecs};
        else this.tableSpecs[0] = tableSpecs;
    }

    public DataTableSpec getSpec(int idx) {
        if (tableSpecs == null) return null;
        if (tableSpecs.length < idx) return null;
        return tableSpecs[idx];
    }


    protected void registerProperty(SettingsModel settingsModel) {
        swingUISettings.add(settingsModel);
    }
}

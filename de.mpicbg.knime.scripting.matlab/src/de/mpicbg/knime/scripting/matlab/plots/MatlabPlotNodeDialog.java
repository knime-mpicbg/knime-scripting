package de.mpicbg.knime.scripting.matlab.plots;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.matlab.MatlabColReformatter;
import de.mpicbg.knime.scripting.matlab.MatlabScriptingBundleActivator;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import javax.swing.*;


/**
 * The MATLAB plot node dialog adds a tab to configure the plot image
 * parameters to the default scripting node dialog.
 * 
 * @author Holger Brandl
 */
public class MatlabPlotNodeDialog extends ScriptingNodeDialog {

    public MatlabPlotNodeDialog(String templateResources, String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new MatlabColReformatter(), useTemplateRepository);

        createNewTab("Output Options");
        addDialogComponent(new DialogComponentNumber(MatlabPlotNodeModel.createPropFigureWidthSetting(), "Width", 10));
        addDialogComponent(new DialogComponentNumber(MatlabPlotNodeModel.createPropFigureHeightSetting(), "Height", 10));

        DialogComponentFileChooser chooser = new DialogComponentFileChooser(MatlabPlotNodeModel.createPropOutputFileSetting(), 
        		"matlabplot.output.file", 
        		JFileChooser.SAVE_DIALOG, "png") {

            // override this method to make the file-selection optional
            @Override
            protected void validateSettingsBeforeSave() throws InvalidSettingsException {
                String value = (String) ((JComboBox) ((JPanel) getComponentPanel().getComponent(0)).getComponent(0)).getSelectedItem();
                ((SettingsModelString) getModel()).setStringValue(value == null ? "" : value);
            }

        };

        addDialogComponent(chooser);
        addDialogComponent(new DialogComponentBoolean(MatlabPlotNodeModel.createOverwriteFileSetting(), "Overwrite existing file"));
    }

    @Override
    public String getTemplatesFromPreferences() {
        return MatlabScriptingBundleActivator.getDefault().getPreferenceStore().getString(MatlabPreferenceInitializer.MATLAB_PLOT_TEMPLATE_RESOURCES);
    }
    
}

package de.mpicbg.tds.knime.scripting.r.plots;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.tds.knime.scripting.r.RColNameReformater;
import de.mpicbg.tds.knime.scripting.r.prefs.RPreferenceInitializer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.*;

import javax.swing.*;
import java.util.Arrays;


/**
 * @author Holger Brandl
 */
public class RPlotNodeDialog extends ScriptingNodeDialog {

    public RPlotNodeDialog(String defaultScript, boolean hasOutput, boolean useTemplateRepository) {
        super(defaultScript, new RColNameReformater(), hasOutput, useTemplateRepository);

        createNewTab("Output Options");
        addDialogComponent(new DialogComponentStringSelection(AbstractRPlotNodeFactory.createPropOutputType(), "File Type", Arrays.asList("png", "jpeg")));
        addDialogComponent(new DialogComponentNumber(AbstractRPlotNodeFactory.createPropFigureWidth(), "Width", 10));
        addDialogComponent(new DialogComponentNumber(AbstractRPlotNodeFactory.createPropFigureHeight(), "Height", 10));

        DialogComponentFileChooser chooser = new DialogComponentFileChooser(AbstractRPlotNodeFactory.createPropOutputFile(), "rplot.output.file", JFileChooser.SAVE_DIALOG, "png") {

            // override this method to make the file-selection optional
            @Override
            protected void validateSettingsBeforeSave() throws InvalidSettingsException {
                String value = (String) ((JComboBox) ((JPanel) getComponentPanel().getComponent(0)).getComponent(0)).getSelectedItem();
                ((SettingsModelString) getModel()).setStringValue(value == null ? "" : value);
            }

        };


        addDialogComponent(chooser);

        addDialogComponent(new DialogComponentBoolean(AbstractRPlotNodeFactory.createOverwriteFile(), "Overwrite existing file"));
    }

    @Override
    public String getTemplatesFromPreferences() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_PLOT_TEMPLATES);
    }

    /*public RPlotNodeDialog(String templateResources, String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, templateResources, false, useTemplateRepository);

        createNewTab("Output Options");
        addDialogComponent(new DialogComponentStringSelection(AbstractRPlotNodeFactory.createPropOutputType(), "File Type", Arrays.asList("png", "jpeg")));
        addDialogComponent(new DialogComponentNumber(AbstractRPlotNodeFactory.createPropFigureWidth(), "Width", 10));
        addDialogComponent(new DialogComponentNumber(AbstractRPlotNodeFactory.createPropFigureHeight(), "Height", 10));

        DialogComponentFileChooser chooser = new DialogComponentFileChooser(AbstractRPlotNodeFactory.createPropOutputFile(), "rplot.output.file", JFileChooser.SAVE_DIALOG, "png") {

            // override this method to make the file-selection optional
            @Override
            protected void validateSettingsBeforeSave() throws InvalidSettingsException {
                String value = (String) ((JComboBox) ((JPanel) getComponentPanel().getComponent(0)).getComponent(0)).getSelectedItem();
                ((SettingsModelString) getModel()).setStringValue(value == null ? "" : value);
            }

        };


        addDialogComponent(chooser);

        addDialogComponent(new DialogComponentBoolean(AbstractRPlotNodeFactory.createOverwriteFile(), "Overwrite existing file"));

        //addDialogComponent(new DialogComponentStringSelection(createPropOutputType(), "Type", Arrays.asList("png", "jpg", "pdf", "svg")));

    }    */


}

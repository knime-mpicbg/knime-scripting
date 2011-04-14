package de.mpicbg.tds.knime.scripting.python.plots;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.scripting.python.PythonColReformatter;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import javax.swing.*;


/**
 * @author Holger Brandl
 */
public class PythonPlotNodeDialog extends ScriptingNodeDialog {

    public PythonPlotNodeDialog(String templateResources, String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new PythonColReformatter(), templateResources, false, useTemplateRepository);

        createNewTab("Output Options");
        addDialogComponent(new DialogComponentNumber(PythonPlotNodeFactory.createPropFigureWidth(), "Width", 10));
        addDialogComponent(new DialogComponentNumber(PythonPlotNodeFactory.createPropFigureHeight(), "Height", 10));

        DialogComponentFileChooser chooser = new DialogComponentFileChooser(PythonPlotNodeFactory.createPropOutputFile(), "matlabplot.output.file", JFileChooser.SAVE_DIALOG, "png") {

            // override this method to make the file-selection optional
            @Override
            protected void validateSettingsBeforeSave() throws InvalidSettingsException {
                String value = (String) ((JComboBox) ((JPanel) getComponentPanel().getComponent(0)).getComponent(0)).getSelectedItem();
                ((SettingsModelString) getModel()).setStringValue(value == null ? "" : value);
            }

        };


        addDialogComponent(chooser);

        addDialogComponent(new DialogComponentBoolean(PythonPlotNodeFactory.createOverwriteFile(), "Overwrite existing file"));

//            addDialogComponent(new DialogComponentStringSelection(createPropOutputType(), "Type", Arrays.asList("png", "jpg", "pdf", "svg")));

    }
}
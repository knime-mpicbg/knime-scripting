package de.mpicbg.knime.scripting.r.node.plot;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.Arrays;


/**
 * @author Holger Brandl
 */
public class RPlotNodeDialog extends ScriptingNodeDialog {

	public RPlotNodeDialog(String defaultScript, boolean enableTemplateRepository) {
		this(defaultScript, enableTemplateRepository, true);
	}

	public RPlotNodeDialog(String defaultScript, boolean enableTemplateRepository, boolean enableOpenExternal) {
		super(defaultScript, new RColumnSupport(), enableTemplateRepository, enableOpenExternal, true);
		
		final SettingsModelString fileSM = AbstractRPlotNodeModel.createOutputFileSM();
		final SettingsModelBoolean overwriteSM = AbstractRPlotNodeModel.createOverwriteSM();
		SettingsModelBoolean writeImageSM = AbstractRPlotNodeModel.createWriteFileSM();

		createNewTab("Output Options");
		addDialogComponent(new DialogComponentStringSelection(AbstractRPlotNodeModel.createImgTypeSM(), "File Type", Arrays.asList("png", "jpeg")));
		addDialogComponent(new DialogComponentNumber(AbstractRPlotNodeModel.createWidthSM(), "Width", 10));
		addDialogComponent(new DialogComponentNumber(AbstractRPlotNodeModel.createHeightSM(), "Height", 10));

		createNewGroup("Save plot to file"); 
		DialogComponentFileChooser chooser = new DialogComponentFileChooser(fileSM, "rplot.output.file", JFileChooser.SAVE_DIALOG, ".png",".jpeg") {

			// override this method to make the file-selection optional
			@SuppressWarnings("rawtypes")
			@Override
			protected void validateSettingsBeforeSave() throws InvalidSettingsException {
				try {
					super.validateSettingsBeforeSave();
				} catch (InvalidSettingsException ise) {
					JComboBox fileComboBox = ((JComboBox) ((JPanel) getComponentPanel().getComponent(0)).getComponent(0));
					final String file = fileComboBox.getEditor().getItem().toString();
					((SettingsModelString) getModel()).setStringValue((file == null || file.trim().length() == 0) ? "" : file);
				}
			}

		};

		addDialogComponent(chooser);
		setHorizontalPlacement(true);
		
		writeImageSM.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean enabled = ((SettingsModelBoolean)e.getSource()).getBooleanValue();
				fileSM.setEnabled(enabled);
				overwriteSM.setEnabled(enabled);
			}
		});
		addDialogComponent(new DialogComponentBoolean(writeImageSM, "Write image to file"));
		addDialogComponent(new DialogComponentBoolean(overwriteSM, "Overwrite existing file"));
		setHorizontalPlacement(false);
		closeCurrentGroup();
	}

	@Override
	public String getTemplatesFromPreferences() {
		return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_PLOT_TEMPLATES);
	}

}

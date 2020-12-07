package de.mpicbg.knime.scripting.python.v2.node.plot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;
import de.mpicbg.knime.scripting.python.v2.plots.AbstractPythonPlotV2NodeModel;

/**
 * 
 * Node Dialog class for 'Python Plot' node
 * 
 * @author Antje Janosch
 *
 */
public class PythonPlotV2NodeDialog extends ScriptingNodeDialog {
	
	/**
	 * constructor
	 * 
	 * @param defaultScript
	 * @param enableTemplateRepository
	 */
	public PythonPlotV2NodeDialog(String defaultScript, boolean enableTemplateRepository) {
		this(defaultScript, enableTemplateRepository, true);
	}
	
	/**
	 * constructor
	 * 
	 * @param defaultScript
	 * @param enableTemplateRepository
	 * @param enableOpenExternal
	 */
	public PythonPlotV2NodeDialog(String defaultScript, boolean enableTemplateRepository, boolean enableOpenExternal) {
		//super(defaultScript, new PythonColumnSupport(), enableTemplateRepository, enableOpenExternal, true);
		super(defaultScript, PythonPlotV2NodeModel.nodeModelConfig);
		
		final SettingsModelString fileSM = AbstractPythonPlotV2NodeModel.createOutputFileSM();
		final SettingsModelBoolean overwriteSM = AbstractPythonPlotV2NodeModel.createOverwriteSM();
		SettingsModelBoolean writeImageSM = AbstractPythonPlotV2NodeModel.createWriteFileSM();
		
		createNewTab("Output Options");
		addDialogComponent(new DialogComponentStringSelection(AbstractPythonPlotV2NodeModel.createImgTypeSM(), "File Type", AbstractPythonPlotV2NodeModel.SUPPORTED_FORMATS));
		addDialogComponent(new DialogComponentNumber(AbstractPythonPlotV2NodeModel.createWidthSM(), "Width", 10));
		addDialogComponent(new DialogComponentNumber(AbstractPythonPlotV2NodeModel.createHeightSM(), "Height", 10));
		addDialogComponent(new DialogComponentNumber(AbstractPythonPlotV2NodeModel.createDpiSM(), "DPI", 10));

		List<String> validExtensions = AbstractPythonPlotV2NodeModel.SUPPORTED_FORMATS.stream().map(i -> ".".concat(i)).collect(Collectors.toList());
		
		createNewGroup("Save plot to file"); 
		DialogComponentFileChooser chooser = new DialogComponentFileChooser(
				fileSM, 
				"pythonplot.output.file", 
				JFileChooser.SAVE_DIALOG,
				validExtensions.toArray(new String[validExtensions.size()])
				) {

			// override this method to make the file-selection optional
			@SuppressWarnings("rawtypes")
			@Override
			protected void validateSettingsBeforeSave() throws InvalidSettingsException {
				super.validateSettingsBeforeSave();
				JComboBox fileComboBox = ((JComboBox) ((JPanel) getComponentPanel().getComponent(0)).getComponent(0));
				final String file = fileComboBox.getEditor().getItem().toString();
				// if there is an empty string the settings model should get "null" as this is no file location
				((SettingsModelString) getModel()).setStringValue((file == null || file.trim().length() == 0) ? null : file);

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

	/**
	 * retrieve python plot templates
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public String getTemplatesFromPreferences() {
		return PythonScriptingBundleActivator.getDefault().getPreferenceStore().getString(PythonPreferenceInitializer.PYTHON_PLOT_TEMPLATE_RESOURCES);
	}

	/**
	 * get template cache folder for this bundle
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected Path getTemplateCachePath() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
        String bundlePath = ScriptingUtils.getBundlePath(bundle).toOSString();
        return Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER);
	}

}

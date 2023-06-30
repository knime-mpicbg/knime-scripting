package de.mpicbg.knime.scripting.core.prefs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;*/
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.ScriptingCoreBundleActivator;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * preference page for jupyter settings
 * 
 * @author Antje Janosch
 *
 */
public class JupyterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/** editor to hold the jupyter installation path */
	private FileFieldEditor ffe;
	/** field editor to hold the python 2 kernel list */
	private JupyterKernelSpecsEditor jksePy2 ;
	/** field editor to hold the python 3 kernel list */
	private JupyterKernelSpecsEditor jksePy3 ;
	/** field editor to hold the R kernel list */
	private JupyterKernelSpecsEditor jkseR ;
		
	/** list of kernel specs */
	private List<JupyterKernelSpec> m_kernelSpecs;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		super.propertyChange(event);
			
		if(event.getSource().equals(ffe) && ffe.isValid()) {
			try {
				updateKernelSpecs(ffe.getStringValue());
				// valid location specs successfully loaded
				jksePy2.updateComboBoxes(m_kernelSpecs);
				jksePy3.updateComboBoxes(m_kernelSpecs);
				jkseR.updateComboBoxes(m_kernelSpecs);
			} catch (IOException | KnimeScriptingException | InterruptedException e) {
				// valid location given but fails to get specs
				jksePy2.updateComboBoxes(null);
				jksePy3.updateComboBoxes(null);
				jkseR.updateComboBoxes(null);
				this.setErrorMessage(e.getMessage());
			}
		}
		if(event.getSource().equals(ffe) && !ffe.isValid()) {
			//no valid location
			jksePy2.updateComboBoxes(null);
			jksePy3.updateComboBoxes(null);
			jkseR.updateComboBoxes(null);
		}
	}
	
	/**
	 * based on a changed jupyter location path => update available kernel specs
	 * 
	 * @param jupyterLocation
	 * @throws IOException
	 * @throws KnimeScriptingException
	 * @throws InterruptedException
	 */
	public void updateKernelSpecs(String jupyterLocation) throws IOException, KnimeScriptingException, InterruptedException {
		
		// use temporary list to fill with new content
		List<JupyterKernelSpec> kernelSpecs = new LinkedList<JupyterKernelSpec>();
		
		ProcessBuilder pb = new ProcessBuilder();
		
		// create json with kernel specs
		if(Utils.isWindowsPlatform()) {
			pb.command("powershell.exe", "-Command", jupyterLocation, "kernelspec", "list", "--json");
		} else {
			pb.command(jupyterLocation, "kernelspec", "list", "--json");
		}
		File outFile = Files.createTempFile("kernelspecs_", ".txt").toFile();
		
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(outFile));
		Process p = pb.start();
		p.waitFor(5, TimeUnit.SECONDS);
				
		try(JsonReader reader = Json.createReader(new FileReader(outFile))) {
			JsonObject jsonObject = reader.readObject();
			JsonObject kernelspecs = jsonObject.getJsonObject("kernelspecs");
			
			if(kernelspecs == null)
				throw new KnimeScriptingException("failed to read JSON. Expected key \"kernelspecs\"");
			
			for(String key : kernelspecs.keySet()) {
				String name = key;
				JsonObject spec = kernelspecs.getJsonObject(name).getJsonObject("spec");
				if(spec == null)
					throw new KnimeScriptingException("failed to read JSON. Expected key \"spec\"");
				String displayName = spec.getString("display_name");
				String language = spec.getString("language");
				
				if(JupyterKernelSpec.isValidLanguage(language))
					kernelSpecs.add(new JupyterKernelSpec(name, displayName, language));
			}
			
		}
		
		if(outFile.exists())
			outFile.delete();
		
		m_kernelSpecs.clear();
		m_kernelSpecs.addAll(kernelSpecs);
	}

	/**
	 * constructor
	 */
	public JupyterPreferencePage() {
		super(FieldEditorPreferencePage.GRID);

	    // Set the preference store for the preference page.
	    IPreferenceStore store = ScriptingCoreBundleActivator.getDefault().getPreferenceStore();
	    setPreferenceStore(store);
	    
	    m_kernelSpecs = new LinkedList<JupyterKernelSpec>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
        
        final String[][] entries = new String[2][2];
        entries[0][1] = ScriptingPreferenceInitializer.JUPYTER_MODE_1;
        entries[0][0] = "lab (recommended)";
        entries[1][1] = ScriptingPreferenceInitializer.JUPYTER_MODE_2;
        entries[1][0] = "notebook";
        
        ffe = new FileFieldEditor(ScriptingPreferenceInitializer.JUPYTER_EXECUTABLE, "Jupyter Executable", true, StringButtonFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
        jksePy2 = new JupyterKernelSpecsEditor(ScriptingPreferenceInitializer.JUPYTER_KERNEL_PY2, "Python 2 kernelspec", getFieldEditorParent(), JupyterKernelSpec.PYTHON_LANG);
        jksePy3 = new JupyterKernelSpecsEditor(ScriptingPreferenceInitializer.JUPYTER_KERNEL_PY3, "Python 3 kernelspec", getFieldEditorParent(), JupyterKernelSpec.PYTHON_LANG);
        jkseR = new JupyterKernelSpecsEditor(ScriptingPreferenceInitializer.JUPYTER_KERNEL_R, "R kernelspec", getFieldEditorParent(), JupyterKernelSpec.R_LANG);
           
        DirectoryFieldEditor nffe = new DirectoryFieldEditor(ScriptingPreferenceInitializer.JUPYTER_FOLDER, "Notebook folder", getFieldEditorParent());
        
        //addField(new BooleanFieldEditor(ScriptingPreferenceInitializer.JUPYTER_USE, "'Open external' as Jupyter notebook", parent));
        addField(ffe);
        addField(jksePy2);
        addField(jksePy3);
        addField(jkseR);
        addField(new ComboFieldEditor(ScriptingPreferenceInitializer.JUPYTER_MODE, "Jupyter mode", entries, getFieldEditorParent()));
        addField(nffe);
        
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench workbench) {
		// nothing to do here
		
	}

}

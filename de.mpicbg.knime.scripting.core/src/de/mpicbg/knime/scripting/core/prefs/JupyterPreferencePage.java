package de.mpicbg.knime.scripting.core.prefs;

import java.io.IOException;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.mpicbg.knime.scripting.core.ScriptingCoreBundleActivator;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;

public class JupyterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private FileFieldEditor ffe;
	private JupyterKernelSpecsEditor jkse ;
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		super.propertyChange(event);
			
		if(event.getSource().equals(ffe) && ffe.isValid()) {
			try {
				jkse.updateKernelSpecs(ffe.getStringValue());
			} catch (IOException | KnimeScriptingException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public JupyterPreferencePage() {
		super(FieldEditorPreferencePage.GRID);

	    // Set the preference store for the preference page.
	    IPreferenceStore store = ScriptingCoreBundleActivator.getDefault().getPreferenceStore();
	    setPreferenceStore(store);
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
        
        final String[][] entries = new String[2][2];
        entries[0][1] = ScriptingPreferenceInitializer.JUPYTER_MODE_1;
        entries[0][0] = "lab (recommended)";
        entries[1][1] = ScriptingPreferenceInitializer.JUPYTER_MODE_2;
        entries[1][0] = "notebook";
        
        ffe = new FileFieldEditor(ScriptingPreferenceInitializer.JUPYTER_EXECUTABLE, "Jupyter Executable", true, parent);
        jkse = new JupyterKernelSpecsEditor(ScriptingPreferenceInitializer.JUPYTER_KERNELS, "Jupyter kernel specs - please assign", parent);
        
        //addField(new BooleanFieldEditor(ScriptingPreferenceInitializer.JUPYTER_USE, "'Open external' as Jupyter notebook", parent));
        addField(ffe);
        addField(jkse);
        addField(new ComboFieldEditor(ScriptingPreferenceInitializer.JUPYTER_MODE, "Jupyter mode", entries, parent));
        addField(new DirectoryFieldEditor(ScriptingPreferenceInitializer.JUPYTER_FOLDER, "Notebook folder", parent));
        
	}

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub
		
	}

}

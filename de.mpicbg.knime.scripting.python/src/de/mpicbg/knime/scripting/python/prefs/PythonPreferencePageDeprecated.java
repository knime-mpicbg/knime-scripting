package de.mpicbg.knime.scripting.python.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;

public class PythonPreferencePageDeprecated extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
    /**
     * Creates a new preference page.
     */
    public PythonPreferencePageDeprecated() {
    	super(GRID);
        setPreferenceStore(PythonScriptingBundleActivator.getDefault().getPreferenceStore());
	}

    /**
     * {@inheritDoc}
     */
	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
        
        addField(new StringFieldEditor(PythonPreferenceInitializer.PYTHON_HOST, "The host where the Python server is running", parent));
        addField(new IntegerFieldEditor(PythonPreferenceInitializer.PYTHON_PORT, "The port on which Python server is listening", parent));

        addField(new BooleanFieldEditor(PythonPreferenceInitializer.PYTHON_LOCAL, "Run python scripts on local system (ignores host/port settings)", parent));
        addField(new StringFieldEditor(PythonPreferenceInitializer.PYTHON_EXECUTABLE, "The path to the local python executable", parent));

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench workbench) {
		// nothing to do here		
	}

}

package de.mpicbg.knime.scripting.core.prefs;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.mpicbg.knime.scripting.core.ScriptingCoreBundleActivator;

/**
 * Scripting Preference page
 * does not contain any settings but is parent of further scripting preference pages
 * 
 * @author Antje Janosch
 *
 */
public class ScriptingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ScriptingPreferencePage() {
		super(FieldEditorPreferencePage.GRID);

	    // Set the preference store for the preference page.
	    IPreferenceStore store = ScriptingCoreBundleActivator.getDefault().getPreferenceStore();
	    setPreferenceStore(store);
	    setDescription("Expand the tree to edit preferences for a specific feature.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
		// nothing to do here
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench workbench) {
		// nothing to do here
	}

}

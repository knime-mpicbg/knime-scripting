package de.mpicbg.knime.scripting.core.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.mpicbg.knime.scripting.core.ScriptingCoreBundleActivator;


public class ScriptingPreferenceInitializer extends AbstractPreferenceInitializer {
	
	public static final String JUPYTER_EXECUTABLE = "jupyter.exec";
    //public static final String JUPYTER_USE = "use.jupyter";   
    //public static final boolean JUPYTER_USE_DFT = Boolean.FALSE;
    
    public static final String JUPYTER_MODE = "jupyter.mode";
    public static final String JUPYTER_MODE_1 = "lab";
    public static final String JUPYTER_MODE_2 = "notebook";
    
    public static final String JUPYTER_FOLDER = "jupyter.folder";

	public static final String JUPYTER_KERNEL_PY2 = "jupyter.py2.kernel";
	public static final String JUPYTER_KERNEL_PY3 = "jupyter.py3.kernel";
	public static final String JUPYTER_KERNEL_R = "jupyter.r.kernel";

	@Override
	public void initializeDefaultPreferences() {
		
		IPreferenceStore store = ScriptingCoreBundleActivator.getDefault().getPreferenceStore();
		
		store.setDefault(JUPYTER_EXECUTABLE, "");
        store.setDefault(JUPYTER_FOLDER, "");
        //store.setDefault(JUPYTER_USE, JUPYTER_USE_DFT);
        store.setDefault(JUPYTER_MODE, JUPYTER_MODE_1);
	}

}

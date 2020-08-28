package de.mpicbg.knime.scripting.python.prefs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.prefs.TemplateTableEditor;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.srv.CommandOutput;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.Python;

/**
 * @author Tom Haux (MPI-CBG), Antje Janosch (MPI-CBG)
 */
public class PythonPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	// might be useful to get a first guess of Python settings - not yet used (2020-08)
	//private final String pythonLocationGuessed;
	//private final String pythonVersionGuessed;
	
	private final Python python = new LocalPythonClient();
	
    /**
     * Creates a new preference page.
     */
    public PythonPreferencePage() {
        super(GRID);
        //pythonLocationGuessed = guessPythonLocation();
        //pythonVersionGuessed = getPythonVersion(pythonLocationGuessed);
        
        setPreferenceStore(PythonScriptingBundleActivator.getDefault().getPreferenceStore());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        String bundlePath = ScriptingUtils.getBundlePath(bundle).toOSString();
        
        Path cacheFolder = Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER);
        Path indexFile = Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER, ScriptingUtils.LOCAL_CACHE_INDEX);

        addField(new RadioGroupFieldEditor(PythonPreferenceInitializer.PYTHON_USE_2, 
        		"Select Python", 1, 
        		new String[][] {
                {"Use Python2:", "py2"},
                {"Use Python3:", "py3"}},
        		parent));   
        
        addField(new FileFieldEditor(PythonPreferenceInitializer.PYTHON_2_EXECUTABLE, "Python2",true, parent));
        addField(new FileFieldEditor(PythonPreferenceInitializer.PYTHON_3_EXECUTABLE, "Python3",true, parent));
        
        addField(new TemplateTableEditor(PythonPreferenceInitializer.PYTHON_TEMPLATE_RESOURCES, "Snippet template resources", cacheFolder, indexFile, parent));
        addField(new TemplateTableEditor(PythonPreferenceInitializer.PYTHON_PLOT_TEMPLATE_RESOURCES, "Plot template resource", cacheFolder, indexFile, parent));
        
        addField(new BooleanFieldEditor(PythonPreferenceInitializer.JUPYTER_USE, "'Open external' as Jupyter notebook (requires valid preference settings under KNIME > Community Scripting > Jupyter Settings)", parent));
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
       // nothing to do here
    }
    
    /**
     * delivers the python version
     * 
     * @return Python executable path as string
     */
	private String guessPythonLocation() {
		
		// try to get python path
		CommandOutput output;
		String[] command;		
		
		try {
			if(Utils.isWindowsPlatform())
				command = new String[] {"powershell.exe", "-Command","where.exe", "python"};
			else 
				command = new String[] {"which","python"};
			output = python.executeCommand(command);
		} catch (Exception re) {
			return null;
		}
	
		if(output.hasStandardOutput()) {			
			return String.join("\n", output.getStandardOutput());
		}

		return null;
	}

	/**
	 * based on a python executable, get its version
	 * 
	 * @param pythonLocationGuessed
	 * @return Python version as string
	 */
	private String getPythonVersion(String pythonLocationGuessed) {
		// try to get python version
		CommandOutput output;
		String[] command;		
		
		try {
			if(Utils.isWindowsPlatform())
				command = new String[] {"powershell.exe", "-Command", pythonLocationGuessed, "--version"};
			else 
				command = new String[] {pythonLocationGuessed,"--version"};
			output = python.executeCommand(command);
		} catch (Exception re) {
			return null;
		}
		
		String outString = ""; 
		
		// older python versions do print the version info to stderr instead of stout, so catch both
		if(output.hasStandardOutput()) {			
			outString= String.join("\n", output.getStandardOutput());
		}
		if(output.hasErrorOutput())
			outString= String.join("\n", output.getStandardOutput());
		
		return outString;
	}
}
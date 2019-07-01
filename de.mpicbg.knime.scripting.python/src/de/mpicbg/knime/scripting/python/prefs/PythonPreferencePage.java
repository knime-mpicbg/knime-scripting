/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as 
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * ------------------------------------------------------------------------
 * 
 * History
 *   19.09.2007 (thiel): created
 */
package de.mpicbg.knime.scripting.python.prefs;

import de.mpicbg.knime.scripting.core.prefs.TemplateTableEditor;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.srv.CommandOutput;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.Python;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;


/**
 * @author Tom Haux (MPI-CBG), Antje Janosch (MPI-CBG)
 */
public class PythonPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final String pythonLocationGuessed;
	private final String pythonVersionGuessed;
	
	private final Python python = new LocalPythonClient();
	
    /**
     * Creates a new preference page.
     */
    public PythonPreferencePage() {
        super(GRID);
        pythonLocationGuessed = guessPythonLocation();
        pythonVersionGuessed = getPythonVersion(pythonLocationGuessed);
        
        setPreferenceStore(PythonScriptingBundleActivator.getDefault().getPreferenceStore());
    }


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



    public void init(final IWorkbench workbench) {
       
    }
    
    /**
     * delivers the python version
     * @return
     */
	private String guessPythonLocation() {
		
		// try to get python path
		CommandOutput output;
		try {
			output = python.executeCommand(new String[]{"which","python"});
		} catch (Exception re) {
			return null;
		}
	
		if(output.hasStandardOutput()) {			
			return String.join("\n", output.getStandardOutput());
		}

		return null;
	}


	private String getPythonVersion(String pythonLocationGuessed) {
		// try to get python version
		CommandOutput output;
		try {
			output = python.executeCommand(new String[]{pythonLocationGuessed,"--version"});
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
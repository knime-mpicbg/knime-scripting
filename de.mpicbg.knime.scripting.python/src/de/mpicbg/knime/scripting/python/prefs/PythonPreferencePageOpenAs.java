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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;


/**
 * @author Tom Haux (MPI-CBG), Antje Janosch (MPI-CBG)
 */
public class PythonPreferencePageOpenAs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Creates a new preference page.
     */
    public PythonPreferencePageOpenAs() {
        super(GRID);

        setPreferenceStore(PythonScriptingBundleActivator.getDefault().getPreferenceStore());
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        
        final String[][] entries = new String[2][2];
        entries[0][1] = PythonPreferenceInitializer.JUPYTER_MODE_1;
        entries[0][0] = "lab (recommended)";
        entries[1][1] = PythonPreferenceInitializer.JUPYTER_MODE_2;
        entries[1][0] = "notebook";
        
        addField(new BooleanFieldEditor(PythonPreferenceInitializer.JUPYTER_USE, "'Open external' as Jupyter notebook", parent));
        addField(new FileFieldEditor(PythonPreferenceInitializer.JUPYTER_EXECUTABLE, "Jupyter Executable", true, parent));
        addField(new ComboFieldEditor(PythonPreferenceInitializer.JUPYTER_MODE, "Jupyter mode", entries, parent));
        addField(new DirectoryFieldEditor(PythonPreferenceInitializer.JUPYTER_FOLDER, "Notebook folder", parent));
    }


    public void init(final IWorkbench workbench) {
      
    }
    
	
}
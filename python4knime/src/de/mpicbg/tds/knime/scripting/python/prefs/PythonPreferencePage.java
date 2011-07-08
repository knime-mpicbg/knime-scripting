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
package de.mpicbg.tds.knime.scripting.python.prefs;

import de.mpicbg.tds.knime.knutils.scripting.prefs.TemplateTableEditor;
import de.mpicbg.tds.knime.scripting.python.PythonScriptingBundleActivator;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * @author Tom Haux (MPI-CBG)
 */
public class PythonPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Creates a new preference page.
     */
    public PythonPreferencePage() {
        super(GRID);

        setPreferenceStore(PythonScriptingBundleActivator.getDefault().getPreferenceStore());
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        addField(new StringFieldEditor(PythonPreferenceInitializer.PYTHON_HOST, "The host where the Python server is running", parent));
        addField(new IntegerFieldEditor(PythonPreferenceInitializer.PYTHON_PORT, "The port on which Python server is listening", parent));

        addField(new BooleanFieldEditor(PythonPreferenceInitializer.PYTHON_LOCAL, "Run python scripts on local system (ignores host/port settings)", parent));
        addField(new StringFieldEditor(PythonPreferenceInitializer.PYTHON_EXECUTABLE, "The path to the local python executable", parent));

        /*addField(new StringFieldEditor(PythonPreferenceInitializer.PYTHON_TEMPLATE_RESOURCES, "Script template resources (;-separated URLs)", parent));
        addField(new StringFieldEditor(PythonPreferenceInitializer.PYTHON_PLOT_TEMPLATE_RESOURCES, "Figure template resources (;-separated URLs)", parent));*/
        addField(new TemplateTableEditor(PythonPreferenceInitializer.PYTHON_TEMPLATE_RESOURCES, "Snippet template resources", parent));
        addField(new TemplateTableEditor(PythonPreferenceInitializer.PYTHON_PLOT_TEMPLATE_RESOURCES, "Plot template resource", parent));
    }


    public void init(final IWorkbench workbench) {
        // nothing to do
    }
}
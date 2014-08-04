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
package de.mpicbg.knime.scripting.matlab.prefs;

import de.mpicbg.knime.scripting.core.prefs.TemplateTableEditor;
import de.mpicbg.knime.scripting.matlab.MatlabScriptingBundleActivator;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * TODO: Make the checkbox act on the server settings (deactivate them when the we use a local server)
 * 
 * @author Holger Brandl
 */
public class MatlabPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Creates a new preference page.
     */
    public MatlabPreferencePage() {
        super(GRID);

        setPreferenceStore(MatlabScriptingBundleActivator.getDefault().getPreferenceStore());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        BooleanFieldEditor local = new BooleanFieldEditor(MatlabPreferenceInitializer.MATLAB_LOCAL,
        		"Run scripts on local MATLAB installation. this overrides the host/port settings bellow",
        		parent);
        
        StringFieldEditor host = new StringFieldEditor(MatlabPreferenceInitializer.MATLAB_HOST,
        		"The host where the Matlab-server is running",
        		parent);
        
        IntegerFieldEditor port = new IntegerFieldEditor(MatlabPreferenceInitializer.MATLAB_PORT,
        		"The port on which Matlab-server is listening",
        		parent);
        
        TemplateTableEditor snippets = new TemplateTableEditor(MatlabPreferenceInitializer.MATLAB_TEMPLATE_RESOURCES,
        		"Snippet template resources",
        		parent);
        
        TemplateTableEditor plots = new TemplateTableEditor(MatlabPreferenceInitializer.MATLAB_PLOT_TEMPLATE_RESOURCES,
        		"Plot template resource",
        		parent);
        
        ComboFieldEditor type = new ComboFieldEditor(MatlabPreferenceInitializer.MATLAB_TYPE,
        		"MATLAB data type to hold the input table.\nUsing something else than dataset might introduce template incompatilities",
        		new String[][]{{"dataset","dataset"}, {"map", "map"}, {"struct", "struct"}},
        		parent);
        
        addField(local);
        addField(host);
        addField(port);
        addField(snippets);
        addField(plots);
        addField(type);
    }


    public void init(final IWorkbench workbench) {
        // nothing to do
    }

}
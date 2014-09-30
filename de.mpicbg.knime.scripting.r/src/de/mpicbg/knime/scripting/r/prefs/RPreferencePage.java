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
package de.mpicbg.knime.scripting.r.prefs;

import de.mpicbg.knime.scripting.core.prefs.TemplateTableEditor;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * @author Kilian Thiel, University of Konstanz
 */
public class RPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Creates a new preference page.
     */
    public RPreferencePage() {
        super(GRID);

        setPreferenceStore(R4KnimeBundleActivator.getDefault().getPreferenceStore());
        setDescription("R4Knime  preferences");
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        addField(new StringFieldEditor(RPreferenceInitializer.R_HOST, "The host where Rserve is running", parent));
        addField(new IntegerFieldEditor(RPreferenceInitializer.R_PORT, "The port on which Rserve is listening", parent));
        addField(new BooleanFieldEditor(RPreferenceInitializer.REPAINT_ON_RESIZE, "Repaint on resize", parent));
        
        addField(new BooleanFieldEditor(RPreferenceInitializer.USE_EVALUATE_PACKAGE, "Enable R-console view (requires 'evaluate' package)", parent));

        addField(new TemplateTableEditor(RPreferenceInitializer.R_SNIPPET_TEMPLATES, "Snippet template resource", parent));
        addField(new TemplateTableEditor(RPreferenceInitializer.R_PLOT_TEMPLATES, "Plot template resource", parent));

        addField(new StringFieldEditor(RPreferenceInitializer.LOCAL_R_PATH, "Location of R on your computer", parent));

    }


    public void init(final IWorkbench workbench) {
        // nothing to do
    }
}
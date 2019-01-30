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
package de.mpicbg.knime.scripting.groovy.prefs;

import de.mpicbg.knime.scripting.core.prefs.TemplateTableEditor;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.groovy.GroovyScriptingBundleActivator;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.prefs.TemplateTableEditor;
import de.mpicbg.knime.scripting.groovy.GroovyScriptingBundleActivator;


/**
 * @author Holger Brandl
 */
public class GroovyScriptingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Creates a new preference page.
     */
    public GroovyScriptingPreferencePage() {
        super(GRID);

        setPreferenceStore(GroovyScriptingBundleActivator.getDefault().getPreferenceStore());
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        String bundlePath = ScriptingUtils.getBundlePath(bundle).toOSString();

        addField(new StringFieldEditor(GroovyScriptingPreferenceInitializer.GROOVY_CLASSPATH_ADDONS, "Additional scripting classpath (;-separated)", parent));
        //addField(new StringFieldEditor(GroovyScriptingPreferenceInitializer.GROOVY_TEMPLATE_RESOURCES, "Template resources (;-separated)", parent));
        addField(new TemplateTableEditor(GroovyScriptingPreferenceInitializer.GROOVY_TEMPLATE_RESOURCES, "Snippet template resource", bundlePath, parent));
    }


    public void init(final IWorkbench workbench) {
        // nothing to do
    }
}
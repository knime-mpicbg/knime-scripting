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
package de.mpicbg.tds.knime.scripting.prefs;

import de.mpicbg.tds.knime.scripting.GroovyScriptingBundleActivator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public class GroovyScriptingPreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String GROOVY_CLASSPATH_ADDONS = "groovy.classpath.addons";
    public static final String GROOVY_TEMPLATE_RESOURCES = "template.resources";


    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = GroovyScriptingBundleActivator.getDefault().getPreferenceStore();

        store.setDefault(GROOVY_CLASSPATH_ADDONS, "{KNIME.HOME}/de.mpicbg.tds.knime.hcstools_1.0.0/hcstools.jar;{KNIME.HOME}/de.mpicbg.tds.knime.hcstools_1.0.0/lib/*");
//        store.setDefault(GROOVY_TEMPLATE_RESOURCES, "http://idisk.mpi-cbg.de/~brandl/scripttemplates/screenmining/Groovy/Groovy-templates.txt");
        store.setDefault(GROOVY_TEMPLATE_RESOURCES, "http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Groovy/Groovy-templates.txt");
    }
}
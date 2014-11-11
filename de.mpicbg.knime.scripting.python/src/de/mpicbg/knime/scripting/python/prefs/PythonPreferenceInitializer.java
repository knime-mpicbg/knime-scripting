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

import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public class PythonPreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String PYTHON_LOCAL = "python.local";

    public static final String PYTHON_HOST = "python.host";
    public static final String PYTHON_PORT = "python.port";

    public static final String PYTHON_EXECUTABLE = "python.exec";

    public static final String PYTHON_TEMPLATE_RESOURCES = "python.template.resources";
    public static final String PYTHON_PLOT_TEMPLATE_RESOURCES = "python.plot.template.resources";


    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

        store.setDefault(PYTHON_LOCAL, Boolean.TRUE);

        store.setDefault(PYTHON_HOST, "localhost");
        store.setDefault(PYTHON_PORT, 1198);

        store.setDefault(PYTHON_EXECUTABLE, "python");

        store.setDefault(PYTHON_TEMPLATE_RESOURCES, "https://raw.githubusercontent.com/knime-mpicbg/scripting-templates/master/knime-scripting-templates/Python/script-templates.txt");
        store.setDefault(PYTHON_PLOT_TEMPLATE_RESOURCES, "https://raw.githubusercontent.com/knime-mpicbg/scripting-templates/master/knime-scripting-templates/Python/figure-templates.txt");

    }
}
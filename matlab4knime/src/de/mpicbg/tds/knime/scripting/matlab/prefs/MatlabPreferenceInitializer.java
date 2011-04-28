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
package de.mpicbg.tds.knime.scripting.matlab.prefs;

import de.mpicbg.tds.knime.scripting.matlab.MatlabScriptingBundleActivator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public class MatlabPreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String MATLAB_HOST = "r.host";
    public static final String MATLAB_PORT = "r.port";

    public static final String MATLB_TEMPLATE_RESOURCES = "template.resources";
    public static final String MATLB_PLOT_TEMPLATE_RESOURCES = "plot.template.resources";


    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = MatlabScriptingBundleActivator.getDefault().getPreferenceStore();

        store.setDefault(MATLAB_HOST, "localhost");
        store.setDefault(MATLAB_PORT, 1198);

//        store.setDefault(MATLB_TEMPLATE_RESOURCES, "file:///Volumes/tds/software+tools/KNIME/script-templates/Matlab/script-templates.txt");
        store.setDefault(MATLB_TEMPLATE_RESOURCES, "http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/script-templates.txt");
        store.setDefault(MATLB_PLOT_TEMPLATE_RESOURCES, "http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/figure-templates.txt");
    }
}
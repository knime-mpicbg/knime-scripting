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
package de.mpicbg.knime.knutils.prefs;

import de.mpicbg.knime.knutils.KnutilsBundleActivator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public class KnutilsPreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String GROOVY_CLASSPATH_ADDONS = "min.samples.means";


    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = KnutilsBundleActivator.getDefault().getPreferenceStore();

        store.setDefault(GROOVY_CLASSPATH_ADDONS, "/Users/brandl/projects/knime/hcstools/lib/hcscore-1.0.jar");
    }
}
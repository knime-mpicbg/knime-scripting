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

import de.mpicbg.knime.scripting.groovy.GroovyScriptingBundleActivator;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public class GroovyScriptingPreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String GROOVY_CLASSPATH_ADDONS = "groovy.classpath.addons";
    public static final String GROOVY_TEMPLATE_RESOURCES = "template.resources";


    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = GroovyScriptingBundleActivator.getDefault().getPreferenceStore();
        
        // find the default class path libs
        File hcstools = null;
        File hcslibs = null;
        for (File f : new File(System.getProperty("osgi.syspath")).listFiles()) {
        	if (f.getName().startsWith("de.mpicbg.knime.hcs.base")) {
						hcstools = new File(f,"hcstools.jar");
        	}
        	if (f.getName().startsWith("de.mpicbg.knime.hcs.libs")) {
						hcslibs = new File(f, "lib"); 
        	}
        }
        
        String defaultClasspath = "";
        if (hcstools != null && hcslibs != null) {
					defaultClasspath = hcstools.getAbsolutePath() + ";" + hcslibs.getAbsolutePath() +  "/*";
        } 

        store.setDefault(GROOVY_CLASSPATH_ADDONS, defaultClasspath);
        store.setDefault(GROOVY_TEMPLATE_RESOURCES, "(\"https://raw.githubusercontent.com/knime-mpicbg/scripting-templates/master/knime-scripting-templates/Groovy/Groovy-templates.txt\",true)");
    }
}
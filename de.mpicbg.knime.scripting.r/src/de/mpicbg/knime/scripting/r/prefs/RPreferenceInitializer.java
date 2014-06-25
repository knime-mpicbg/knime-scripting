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

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;

import java.io.File;


/**
 * @author Kilian Thiel, University of Konstanz
 */
public class RPreferenceInitializer extends AbstractPreferenceInitializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RPreferenceInitializer.class);

    /**
     * Preference key for the path to the R executable setting.
     */
//    public static final String PREF_R_PATH = "knime.r.path";
    public static final String REPAINT_ON_RESIZE = "repaint.on.resize";

    public static final String R_PLOT_TEMPLATES = "templates.figures";
    public static final String R_SNIPPET_TEMPLATES = "template.snippets";
    public static final String R_OPENINR_TEMPLATES = "templates.openinr";

    public static final String R_HOST = "r.host";
    public static final String R_PORT = "r.port";

    public static final String LOCAL_R_PATH = "local.r.path";


    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = R4KnimeBundleActivator.getDefault().getPreferenceStore();

//        File rPath = RCorePlugin.getRExecutable();
//        if (rPath != null) {
//            LOGGER.info("Default R executable: " + rPath.getAbsolutePath());
//            store.setDefault(PREF_R_PATH, rPath);
//
//        } else {
//            store.setDefault(PREF_R_PATH, "");
//        }

        store.setDefault(REPAINT_ON_RESIZE, false);
        store.setDefault(R_HOST, "localhost");
        store.setDefault(R_PORT, 6311);

//        store.setDefault(R_PLOT_TEMPLATES, "http://idisk.mpi-cbg.de/~brandl/scripttemplates/screenmining/R/figure-templates.txt");
//        store.setDefault(R_SNIPPET_TEMPLATES, "http://idisk.mpi-cbg.de/~brandl/scripttemplates/screenmining/R/snippet-templates.txt");
//        store.setDefault(R_OPENINR_TEMPLATES, "http://idisk.mpi-cbg.de/~brandl/scripttemplates/screenmining/R/openinr-templates.txt");
        store.setDefault(R_PLOT_TEMPLATES, "http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/figure-templates.txt");
        store.setDefault(R_SNIPPET_TEMPLATES, "http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/snippet-templates.txt");
        store.setDefault(R_OPENINR_TEMPLATES, "http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/openinr-templates.txt");

        String defaultRPath = "R";

        if (Utils.isMacOSPlatform()) {
            defaultRPath = "/Applications/R64.app/";
        }

        if (Utils.isWindowsPlatform()) {
            defaultRPath = findRExecutable();
        }
        store.setDefault(LOCAL_R_PATH, defaultRPath);

    }


    private String findRExecutable() {
        File appDir = new File(System.getenv("ProgramFiles"));
        //list all and try to find r

        if (!appDir.isDirectory()) {
            System.err.println("could not application directory 'ProgramFiles'");
            return "";
        }

        for (File file : appDir.listFiles()) {
            if (!file.getName().matches("R")) {
                continue;
            }
            File[] files = file.listFiles();
            if (files.length > 0) {
                return files[files.length - 1].getAbsolutePath() + File.separator + "bin/RGui.exe";
            }
        }


        return "";
    }


    /**
     * Returns the path to the R executable.
     *
     * @return the path
     */
    public static String getRPath() {
        final IPreferenceStore pStore = R4KnimeBundleActivator.getDefault().getPreferenceStore();
        return pStore.getString(REPAINT_ON_RESIZE);
    }
}
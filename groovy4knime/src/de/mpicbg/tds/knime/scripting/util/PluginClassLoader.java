/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package de.mpicbg.tds.knime.scripting.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * The class loader for a plugin (extending URLClassLoader). Since a plugin might depend on other plugins the URLs of
 * these plugins are also added to the current class loader.
 *
 * @author Ingo Mierswa
 */
public class PluginClassLoader extends URLClassLoader {

    static {
        // register the protocol-handler to create urls for jar-embedded jars
        JarJarURLConnection.register();
    }


    public PluginClassLoader(URL[] urls, ClassLoader classLoader) {
        super(urls, classLoader);

        URL pluginJarURL = urls[0];

        List<URL> urlList = collectDependcyJarURLs(pluginJarURL);
        for (URL url : urlList) {
            addURL(url);
        }
    }


    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }


    public Class<?> loadClass(String name)
            throws ClassNotFoundException {
        Class loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            try {
                loadedClass = findClass(name);
            } catch (ClassNotFoundException e) {
                // Swallow exception
                //does not exist locally
            }

            if (loadedClass == null) {
                loadedClass = super.loadClass(name);
            }
        }
        return loadedClass;
    }


    private List<URL> collectDependcyJarURLs(URL pluginJarURL) {
        List<URL> dependencyURLs = new ArrayList<URL>();

        try {
            JarFile jar = new JarFile(new File(pluginJarURL.toURI()));

            Enumeration<JarEntry> jarentries = jar.entries();
            while (jarentries.hasMoreElements()) {
                JarEntry jarEntry = jarentries.nextElement();

                String jarEntryName = jarEntry.getName();
                if (jarEntryName.endsWith(".jar")) {
                    // create a url for it
                    String pluginInDepUrl = "jar:jarjar:" + pluginJarURL.toString() + "^/" + jarEntryName + "!/";
                    dependencyURLs.add(new URL(pluginInDepUrl));
//                    dependencyURLs.add(new URL(null, pluginInDepUrl, new JarJarURLStreamHandler()));
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        return dependencyURLs;
    }


    @Override
    public String toString() {
        return "PluginClassLoader (" + Arrays.asList(getURLs()) + ")";
    }


    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException {

        URL[] urls = new URL[]{new File("/Volumes/tds/software+tools/KNIME/tdsbuilds/knime_2.1.2_current/plugins/de.mpicbg.tds.knime.hcstools_1.0.0.jar").toURL()};
//        URL[] urls = new URL[]{new File("/Users/brandl/projects/knime/hcstools/lib/tdscore-1.0.jar").toURL()};

        PluginClassLoader loader = new PluginClassLoader(urls, PluginClassLoader.class.getClassLoader());

        for (URL url : loader.getURLs()) {
            System.err.println("url is " + url);
        }

        // find class
        Class<?> aClass = loader.loadClass("de.mpicbg.tds.knime.hcstools.utils.AttributeStatistics");
//        Class<?> aClass = loader.loadClass("de.mpicbg.tds.core.util.ArrayScanHelper");
        System.err.println("class is " + aClass);

        Class<?> anotherClass = loader.loadClass("de.mpicbg.tds.core.util.ArrayScanHelper");
        System.err.println("class is " + anotherClass);

    }
}

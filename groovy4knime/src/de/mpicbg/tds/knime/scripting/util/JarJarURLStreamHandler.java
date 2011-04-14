package de.mpicbg.tds.knime.scripting.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class JarJarURLStreamHandler extends URLStreamHandler {

    protected URLConnection openConnection(URL u) throws IOException {
        return new JarJarURLConnection(u);
    }


    private int indexOfBangSlash(String spec) {
        int indexOfBang = spec.length();

        while ((indexOfBang = spec.lastIndexOf(JarJarURLConnection.SEPARATOR_CHAR, indexOfBang)) != -1) {
            if ((indexOfBang != (spec.length() - 1)) && (spec.charAt(indexOfBang + 1) == '/')) {
                return indexOfBang + 1;
            } else {
                indexOfBang--;
            }
        }
        return -1;
    }


    @SuppressWarnings({"deprecation"})
    protected void parseURL(URL url, String spec,
                            int start, int limit) {
        String file = null;
        String ref = null;
        // first figure out if there is an anchor
        int refPos = spec.indexOf('#', limit);
        boolean refOnly = refPos == start;

        if (refPos > -1) {
            ref = spec.substring(refPos + 1, spec.length());
            if (refOnly) {
                file = url.getFile();
            }
        }
        // then figure out if the spec is
        // 1. absolute (jarjar:)
        // 2. relative (i.e. url + foo/bar/baz.ext)
        // 3. anchor-only (i.e. url + #foo), which we already did (refOnly)
        boolean absoluteSpec = false;

        if (spec.length() >= 7) {
            absoluteSpec = spec.substring(0, 7).equalsIgnoreCase("jarjar:");
        }
        spec = spec.substring(start, limit);

        if (absoluteSpec) {
            file = parseAbsoluteSpec(spec);
        } else if (!refOnly) {
            file = parseContextSpec(url, spec);

            // Canonize the result after the bangslash
            int bangSlash = indexOfBangSlash(file);
            String toBangSlash = file.substring(0, bangSlash);
            String afterBangSlash = file.substring(bangSlash);

            file = toBangSlash + afterBangSlash;
        }
        file = file != null ? "jar:" + file.replaceFirst("\\" + JarJarURLConnection.SEPARATOR, "!/") : null;
        setURL(url, "jarjar", "", -1, file, ref);
    }


    @SuppressWarnings({"UnusedAssignment", "UnusedDeclaration"})
    private String parseAbsoluteSpec(String spec) {
        @SuppressWarnings("unused")
        URL url = null;
        int index = -1;

        // check for !/
        if ((index = indexOfBangSlash(spec)) == -1) {
            throw new NullPointerException("no " + JarJarURLConnection.SEPARATOR + " in spec");
        }
        // test the inner URL
        try {
            String innerSpec = spec.substring(0, index - 1);

            url = new URL(innerSpec);
        } catch (MalformedURLException e) {
            throw new NullPointerException("invalid url: " + spec + " (" + e + ")");
        }
        return spec;
    }


    private String parseContextSpec(URL url, String spec) {
        String ctxFile = url.getFile();

        // if the spec begins with /, chop up the jar back !/
        if (spec.startsWith("/")) {
            int bangSlash = indexOfBangSlash(ctxFile);

            if (bangSlash == -1) {
                throw new NullPointerException("malformed " + "context url:" + url + ": no " + JarJarURLConnection.SEPARATOR);
            }
            ctxFile = ctxFile.substring(0, bangSlash);
        }
        if (!ctxFile.endsWith("/") && (!spec.startsWith("/"))) {
            // chop up the last component
            int lastSlash = ctxFile.lastIndexOf('/');

            if (lastSlash == -1) {
                throw new NullPointerException("malformed " + "context url:" + url);
            }
            ctxFile = ctxFile.substring(0, lastSlash + 1);
        }
        return (ctxFile + spec);
    }
}

package de.mpicbg.knime.scripting.groovy.utils; /*******************************************************************************
 * Copyright (c) 2009 Pavel Savara as part of Robocode project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/cpl-v10.html
 *
 * Contributors:
 *     Pavel Savara
 *     - JarJarURLStreamHandler is just tweaked version of jar handler
 *       from OpenJDK, license below
 *******************************************************************************/

/*
 * Copyright 1997-2000 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;


/**
 * @author Pavel Savara
 */
public class JarJarURLConnection extends URLConnection {

    private URLConnection connection;
    public final static char SEPARATOR_CHAR = '^';
    public final static String SEPARATOR = SEPARATOR_CHAR + "/";


    public JarJarURLConnection(URL url)
            throws IOException {
        super(url);
        final String file = url.getFile();
        URL inner = new URL(file);

        connection = inner.openConnection();
    }


    public void connect() throws IOException {
        if (!connected) {
            connection.connect();
            connected = true;
        }
    }


    public InputStream getInputStream() throws IOException {
        connect();
        return connection.getInputStream();
    }


    public static void register() {
        URL.setURLStreamHandlerFactory(new JarJarURLStreamHandlerFactory());
    }


    public static class JarJarURLStreamHandlerFactory implements URLStreamHandlerFactory {

        public URLStreamHandler createURLStreamHandler(String protocol) {
            if (protocol.equals("jarjar")) {
                return new JarJarURLStreamHandler();
            }
            return null;
        }
    }


}

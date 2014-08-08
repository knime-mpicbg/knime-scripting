/*
 * Copyright (c) 2011.
 * Max Planck Institute of Molecular Cell Biology and Genetics, Dresden
 *
 * This module is distributed under the BSD-License. For details see the license.txt.
 *
 * It is the obligation of every user to abide terms and conditions of The MathWorks, Inc. Software License Agreement.
 * In particular Article 8 “Web Applications”: it is permissible for an Application created by a Licensee of the
 * NETWORK CONCURRENT USER ACTIVATION type to use MATLAB as a remote engine with static scripts.
 */

package de.mpicbg.knime.scripting.matlab.srv;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * An lean interface to Matlab. The used implementation depends on the current context and is configurable by the user.
 *
 * @author Holger Brandl, Tom Haux
 */
@Deprecated
public interface MatlabWeb {

    public int DEFAULT_PORT = 1198;
    public String DEFAULT_HOST = "localhost";
    public String REGISTRY_NAME = "MatlabServer";


    void eval(String evalCmd);

    Object feval(String funName, Object[] args);

    void saveWorkspace(String fileName);

    void clearWorkspace(String... fields);

    void clearAll();

    void shutDown();

    //
    // SETTERS
    //
    void setScalar(String varName, double v);

    void setVector(String varName, double[] v);

    void setMatrix(String varName, double[][] m);

    void setString(String varName, String s);

    void setStringList(String varName, List<String> strings);

    //
    // GETTERS
    //
    double getScalar(String varName);

    double[] getVector(String varName);

    double[][] getMatrix(String varName);

    String getString(String varName);

    String[] getStringList(String varName);


    //
    // File handling
    //
    public File createTempFile(String prefix, String suffix);

    public String getFilePath(File file);

    public boolean deleteFile(File file);

    public int openFile(File file) throws IOException;

    public byte[] readFile(int descriptor) throws IOException;

    public void writeFile(int descriptor, byte[] bytes) throws IOException;

    public void closeFile(int descriptor) throws IOException;
}

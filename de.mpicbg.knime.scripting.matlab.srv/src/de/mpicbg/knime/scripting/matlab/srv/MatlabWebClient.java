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

import gnu.cajo.utils.extra.TransparentItemProxy;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Author: Tom Haux, Holger Brandl, Felix Meyenhofer
 * Date: 2/23/11
 * Time: 5:17 PM
 */
@Deprecated
public class MatlabWebClient implements MatlabWeb {

    private MatlabWeb matlab;

    //
    // Constructors for MatlabWebClient that communicates with the MatlabServer
    //

    public MatlabWebClient() {
        this(MatlabWeb.DEFAULT_HOST, MatlabWeb.DEFAULT_PORT);
    }

    public MatlabWebClient(String serverName, int serverPort) {
        try {
            String url = "//" + serverName + ":" + serverPort + "/" + MatlabWeb.REGISTRY_NAME;
            matlab = (MatlabWeb) TransparentItemProxy.getItem(url, new Class[]{MatlabWeb.class});
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    //
    // Utilities
    //
    public boolean isConnected() {
        try {
            matlab.getScalar("+exist('ichbinsichernichtda', 'var')");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean isLocked() {
        try {
            double status = matlab.getScalar("+exist('serverISlocked', 'var')");
            if (status == 1) {
                return true;
            } else if (status == 0) {
                return false;
            } else {
                throw new RuntimeException("The status retruned by matlab was: '" + status + "' (0 or 1 expected");
            }
        } catch (Exception e) {
            return true;
        }
    }

    public void lockServer() {
        matlab.eval("serverISlocked=true;");
    }

    public void unlockServer() {
        matlab.eval("clear serverISlocked");
    }

    public String[] assessWorkspace() {
        matlab.eval("workspaceVariableNames = who;");
        String[] workspace = matlab.getStringList("workspaceVariableNames");
        matlab.eval("clear workspaceVariableNames;");
        return workspace;
    }

    public void recessWorkspace(String[] workspace) {
        matlab.eval("workspaceVariableNames = who;");
        String[] currVars = matlab.getStringList("workspaceVariableNames");
        List<String> varList = Arrays.asList(workspace);
        String statement = "clear ";
        for (String currVar : currVars) {
            if (!varList.contains(currVar)) {
                statement += currVar + " ";
            }
        }
        if (statement.length() > 6) {
            matlab.eval(statement);
        }
    }

    public String executeScript(String script) {
        String errmsg = "";
        script = "try;" + script + ";catch errmsg;end;";
        matlab.eval(script);
        double status = matlab.getScalar("exist('errmsg', 'var');");
        if (status == 1) {
            String id = matlab.getString("errmsg.identifier");
            String msg = matlab.getString("errmsg.message");
            errmsg = "The script execution failed.\n\t\t" + id + "\n\t\t" + msg;
        }
        return errmsg;
    }


    //
    // Interface
    //
    public void eval(String evalCmd) {
        matlab.eval(evalCmd);
    }

    public Object feval(String funName, Object[] args) {
        return matlab.feval(funName, args);
    }

    public void clearWorkspace(String... fields) {
        matlab.clearWorkspace(fields);
    }

    public void saveWorkspace(String fileName) {
        matlab.saveWorkspace(fileName);
    }

    public void clearAll() {
        matlab.clearAll();
    }

    public void shutDown() {
        matlab.shutDown();
    }


    public void setScalar(String varName, double v) {
        matlab.setScalar(varName, v);
    }

    public void setVector(String varName, double[] v) {
        matlab.setVector(varName, v);
    }

    public void setMatrix(String varName, double[][] m) {
        matlab.setMatrix(varName, m);
    }

    public void setString(String varName, String s) {
        matlab.setString(varName, s);
    }

    public void setStringList(String varName, List<String> strings) {
        matlab.setStringList(varName, strings);
    }


    public double getScalar(String varName) {
        return matlab.getScalar(varName);
    }

    public double[] getVector(String varName) {
        return matlab.getVector(varName);
    }

    public double[][] getMatrix(String varName) {
        return matlab.getMatrix(varName);
    }

    public String getString(String varName) {
        return matlab.getString(varName);
    }

    public String[] getStringList(String varName) {
        return matlab.getStringList(varName);
    }


    //
    // File handling
    //
    public File createTempFile(String prefix, String suffix) {
        return matlab.createTempFile(prefix, suffix);
    }

    public String getFilePath(File file) {
        return matlab.getFilePath(file);
    }

    public boolean deleteFile(File file) {
        return matlab.deleteFile(file);
    }

    public int openFile(File file) throws IOException {
        return matlab.openFile(file);
    }

    public byte[] readFile(int descriptor) throws IOException {
        return matlab.readFile(descriptor);
    }

    public void writeFile(int descriptor, byte[] bytes) throws IOException {
        matlab.writeFile(descriptor, bytes);
    }

    public void closeFile(int descriptor) throws IOException {
        matlab.closeFile(descriptor);
    }

}

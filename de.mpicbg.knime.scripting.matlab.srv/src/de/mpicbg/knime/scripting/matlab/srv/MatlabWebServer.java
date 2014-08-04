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

import com.mathworks.jmi.CompletionObserver;

import de.mpicbg.knime.scripting.matlab.srv.utils.MatlabUtilities;
import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.*;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * Authors: Holger Brandl, Tom Haux, Felix Meyenhofer
 * Date: 2/23/11
 * Time: 4:54 PM
 */

public class MatlabWebServer implements MatlabWeb {

    com.mathworks.jmi.Matlab matlab;
    private ServerFileMap map = new ServerFileMap();

    //
    // Constructors
    //
    public MatlabWebServer(int port) {
        try {
            System.out.println("Configuring on port: " + port);
            Remote.config(null, port, null, 0);
            System.out.println("Registering with name: " + REGISTRY_NAME);
            ItemServer.bind(this, REGISTRY_NAME);
            System.out.println("MatlabWeb server started...");
            matlab = new com.mathworks.jmi.Matlab();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Construct a server for local use only--does not initialize the client/server framework
    protected MatlabWebServer(boolean local) {
    }

    public MatlabWebServer() {
        new MatlabWebServer(DEFAULT_PORT);
    }


    //
    // Interface
    //
    public Object feval(final String function, final Object[] args) {
        try {
            final Completion completion = new Completion();

            com.mathworks.jmi.Matlab.whenMatlabReady(new Runnable() {
                public void run() {
                    matlab.fevalConsoleOutput(function, args, 1, completion);
                }
            });
            return completion.getReturnValue();
        } catch (Exception e) {
            System.err.println(getClass() + " could not call : " + function + "(" + Arrays.toString(args) + ")");
        }

        return null;
    }

    public void eval(String matlabCmd) {
        try {
            Completion completion = new Completion();

            matlab.evalConsoleOutput(matlabCmd, completion);
            completion.getReturnValue();
            return;
        } catch (Exception e) {
            System.err.println(getClass() + " problem occured while evaluating command : " + matlabCmd);
        }
    }

    public void saveWorkspace(String fileName) {
        GregorianCalendar cal = new GregorianCalendar();

        if (fileName == null)
            fileName = "mworkspace" + cal.getTime().toString().replace(':', '-') + ".mat";

        assert new File(fileName).getAbsoluteFile().getParentFile().isDirectory() : "directory where matlab workspace should be saved does not exist";

        eval("save('" + fileName + "');");
    }

    public void clearWorkspace(String... fields) {
        for (String varName : fields)
            eval("clear " + varName);
    }

    public void clearAll() {
        eval("clear variables");
        eval("clear functions");
        eval("clear global");
        eval("clear all");
    }

    public void shutDown() {
        eval("quit"); // TODO It would be nicer to just delete the class.
    }


    public void setScalar(String varName, double v) {
        eval(varName + " = " + v + ";");
    }

    public void setVector(String varName, double[] vector) {
        StringBuffer stringVec = MatlabUtilities.convert2StringVector(vector);
        String cmd = varName + " = " + stringVec.toString() + ";";
        eval(cmd);
    }

    public void setMatrix(String varName, double[][] m) {
        StringBuffer stringMatrix = new StringBuffer(varName + " = [");
        for (int i = 0; i < m.length; i++) {
            double[] vector = m[i];
            stringMatrix.append(MatlabUtilities.convert2StringVector(vector));
            if (i != (m.length - 1)) {
                stringMatrix.append(";");
            }
        }
        eval(stringMatrix.append("];").toString());
    }

    public void setString(String varName, String s) {
        eval(varName + " = '" + s + "';");
    }

    public void setStringList(String varName, List<String> strings) {
        StringBuffer str = new StringBuffer(varName + "={");
        for (String string : strings) {
            str.append("'").append(string).append("';");
        }
        String cmd = str.toString().substring(0, str.length() - 1) + "};";
        eval(cmd);
    }


    public double getScalar(String varName) {
        Object returnVal = feval("eval", new Object[]{varName});
        if (returnVal instanceof int[]) {
            return ((int[]) returnVal)[0];
        }
        if (returnVal instanceof float[]) {
            return ((float[]) returnVal)[0];
        }
        if (returnVal instanceof double[]) {
            return ((double[]) returnVal)[0];
        }
        return -Double.MAX_VALUE;
    }

    public double[] getVector(String varName) {
        Object returnVal = feval("eval", new Object[]{varName});
        return ((double[]) returnVal);
    }

    public double[][] getMatrix(String varName) {
        Object returnVal = feval("eval", new Object[]{varName});
        double[] matrixData = (double[]) returnVal;
        // note: for whatever reason, the returnVal is a vector not a matrix. Thus, we need to reshape it.
        eval("tmplen= size(" + varName + ",1);");
        int height = (int) getScalar("tmplen");
        int width = matrixData.length / height;
        eval("clear tmplen");
        // now reshape the matrix into its correct geometry
        double[][] matrix = new double[height][width];
        for (int i = 0; i < matrixData.length; i++) {
            int curRowNum = i / width;
            System.err.println("row num is " + curRowNum);
            matrix[curRowNum][i - (curRowNum * width)] = matrixData[i];
        }
        return matrix;
    }

    public String getString(String varName) {
        Object returnVal = feval("eval", new Object[]{varName});
        return (String) returnVal;
    }

    public String[] getStringList(String varName) {
        Object returnVal = feval("eval", new Object[]{varName});
//        System.err.println("stringlist is" + returnVal);
//        System.err.println("stringlist type is" + returnVal.getClass());
        return (String[]) returnVal;
    }


    public File createTempFile(String prefix, String suffix) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit();
        } catch (IOException e) {
        }
        return tempFile;
    }

    public String getFilePath(File file) {
        return file.getAbsolutePath();
    }

    public boolean deleteFile(File file) {
        return file != null ? file.delete() : true;
    }

    public int openFile(File file) throws IOException {
        return map.add(file);
    }

    public byte[] readFile(int descriptor) throws IOException {
        ServerFile file = map.get(descriptor);
        return file.read();
    }

    public void writeFile(int descriptor, byte[] bytes) throws IOException {
        ServerFile file = map.get(descriptor);
        file.write(bytes);
    }

    public void closeFile(int descriptor) throws IOException {
        ServerFile file = map.get(descriptor);
        file.close();
        map.remove(descriptor);
    }


    /**
     * Maintain a map of descriptors to ServerFile references.
     */
    class ServerFileMap {

        private int currentDescriptor = 0;
        private Hashtable<String, ServerFile> map = new Hashtable<String, ServerFile>();

        public ServerFile get(int descriptor) {
            return map.get(Integer.toString(descriptor));
        }

        public int add(File file) {
            return add(new ServerFile(file));
        }

        public int add(ServerFile file) {
            String descriptor = Integer.toString(currentDescriptor);
            map.put(descriptor, file);
            return currentDescriptor++;
        }

        public void remove(int descriptor) {
            map.remove(Integer.toString(descriptor));
        }

    }


    class ServerFile {

        // Only one of these will be created
        private InputStream input;
        private OutputStream output;
        // Buffer used for read operations
        private byte[] buffer = new byte[8192];
        private File file;


        public ServerFile(File file) {
            this.file = file;
        }


        public byte[] read() throws IOException {
            // Create the stream if this is the first file operation
            if (input == null) input = new BufferedInputStream(new FileInputStream(file));

            int n = input.read(buffer);
            byte[] b = buffer;
            if (n == -1) b = new byte[0];
            else if (n < buffer.length) b = Arrays.copyOf(buffer, n);

            return b;
        }

        public void write(byte[] bytes) throws IOException {
            // Create the stream if this is the first file operation
            if (output == null) output = new BufferedOutputStream(new FileOutputStream(file));
            output.write(bytes);
        }

        public void close() throws IOException {
            // Close whichever stream was created
            if (input != null) input.close();
            if (output != null) output.close();
        }

    }


    //
    // Class to monitor the matlab processes
    //
    private class Completion<T> implements CompletionObserver {

        private T returnValue;
        private Semaphore semaphore = new Semaphore(0);

        /*
        * @see com.mathworks.jmi.CompletionObserver#completed(int,
        *      java.lang.Object)
        */
        @SuppressWarnings("unchecked")
        public void completed(int arg0, Object arg1) {
            returnValue = (T) arg1;
            semaphore.release();
        }

        T getReturnValue() {
            try {
                semaphore.acquire();
                return returnValue;
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

}

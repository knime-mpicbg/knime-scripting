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

package de.mpicbg.knime.scripting.matlab.srv.utils;

import java.io.*;
import java.util.Arrays;

import de.mpicbg.knime.scripting.matlab.srv.MatlabWeb;

/**
 * Encapsulates client and server temp files to simplify create/delete/copy operations
 */
public class MatlabTempFile {

    private File clientFile, serverFile;
    private MatlabWeb matlab;

    public MatlabTempFile(MatlabWeb matlab, String prefix, String suffix) throws IOException {
        this.matlab = matlab;

        clientFile = File.createTempFile(prefix, suffix);
        clientFile.deleteOnExit();
        serverFile = matlab.createTempFile(prefix, suffix);
    }

    public MatlabTempFile(MatlabWeb matlab, File inputFile) throws IOException {
        this.matlab = matlab;

        clientFile = inputFile;
        serverFile = matlab.createTempFile("matlabWebServer", ".tmp");
    }

    public File getClientFile() {
        return clientFile;
    }

    public File getServerFile() {
        return serverFile;
    }

    public String getClientPath() {
        return clientFile.getAbsolutePath();
    }

    public String getServerPath() {
        return matlab.getFilePath(serverFile);
    }

    public void upload() {
        long time = System.currentTimeMillis();

        // Read the file into a byte array and pass it to the server
        int descriptor = -1;
        BufferedInputStream bis = null;

        try {
            byte[] buffer = new byte[8192];
            descriptor = matlab.openFile(serverFile);
            bis = new BufferedInputStream(new FileInputStream(clientFile));
            int bytesRead = 0;

            // Keep reading unil we hit the end of the file
            // when the end of the stream has been reached, -1 is returned
            while ((bytesRead = bis.read(buffer)) != -1) {
                byte[] b = buffer;
                if (bytesRead < buffer.length) b = Arrays.copyOf(buffer, bytesRead);
                matlab.writeFile(descriptor, b);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (descriptor != -1) matlab.closeFile(descriptor);
                if (bis != null) bis.close();
            } catch (IOException e) {
            }

//            abstractMatlabScriptingNodeModel.logger.debug("Uploading file=" + clientFile.getAbsolutePath() + ", length=" + clientFile.length());
//            abstractMatlabScriptingNodeModel.logger.debug("Upload completed in: " + (System.currentTimeMillis() - time) + " milliseconds");
        }
    }

    public void fetch() {
        long time = System.currentTimeMillis();
        // Get a byte array from the server and write it to a file
        // Read the file into a byte array and pass it to the server
        int descriptor = -1;
        BufferedOutputStream bos = null;

        try {
            descriptor = matlab.openFile(serverFile);

            bos = new BufferedOutputStream(new FileOutputStream(clientFile));
            while (true) {
                byte[] bytes = matlab.readFile(descriptor);
                if (bytes.length == 0) break;
                bos.write(bytes);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (descriptor != -1) matlab.closeFile(descriptor);
                if (bos != null) bos.close();
            } catch (IOException e) {
            }
        }

//        abstractMatlabScriptingNodeModel.logger.debug("Downloading file=" + serverFile.getAbsolutePath() + ", length=" + clientFile.length());
//        abstractMatlabScriptingNodeModel.logger.debug("Download completed in: " + (System.currentTimeMillis() - time) + " milliseconds");
    }

    public void delete() {
        clientFile.delete();
        matlab.deleteFile(serverFile);
    }
}

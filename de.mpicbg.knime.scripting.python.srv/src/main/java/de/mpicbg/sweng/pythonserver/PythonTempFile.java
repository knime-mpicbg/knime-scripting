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

package de.mpicbg.sweng.pythonserver;

import java.io.*;
import java.util.Arrays;

/**
 * Encapsulates client and server temp files to simplify create/delete/copy operations
 */
public class PythonTempFile {
    private File clientFile, serverFile;

    private boolean isLocal = false;
    private Python python;

    public PythonTempFile(Python python, String prefix, String suffix) throws IOException {
        this.python = python;

        clientFile = File.createTempFile(prefix, suffix);
        clientFile.deleteOnExit();
        isLocal = python instanceof LocalPythonClient;
        serverFile = isLocal ? clientFile : python.createTempFile(prefix, suffix);
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
        return python.getFilePath(serverFile);
    }

    public void upload() {
        // For local clients there is nothing to do
        if (isLocal) return;

        long time = System.currentTimeMillis();

        // Read bytes from the client file and pass them to the server until the entire file has ben transferred
        int descriptor = -1;
        BufferedInputStream bis = null;

        try {
            byte[] buffer = new byte[8192];

            descriptor = python.openFile(serverFile);

            bis = new BufferedInputStream(new FileInputStream(clientFile));
            int bytesRead = 0;

            // Keep reading unil we hit the end of the file
            // when the end of the stream has been reached, -1 is returned
            while ((bytesRead = bis.read(buffer)) != -1) {
                byte[] b = buffer;
                if (bytesRead < buffer.length) b = Arrays.copyOf(buffer, bytesRead);
                python.writeFile(descriptor, b);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (descriptor != -1) python.closeFile(descriptor);
                if (bis != null) bis.close();
            } catch (IOException e) {
            }

//            logger.debug("Uploading file=" + clientFile.getAbsolutePath() + ", length=" + clientFile.length());
//            logger.debug("Upload completed in: " + (System.currentTimeMillis() - time) + " milliseconds");
        }
    }

    public void fetch() {
        // For local clients there is nothing to do
        if (isLocal) return;

        long time = System.currentTimeMillis();

        // Get bytes from the server and write them to the client until the entire file has been trasferred
        int descriptor = -1;
        BufferedOutputStream bos = null;

        try {
            descriptor = python.openFile(serverFile);

            bos = new BufferedOutputStream(new FileOutputStream(clientFile));
            while (true) {
                byte[] bytes = python.readFile(descriptor);
                if (bytes.length == 0) break;
                bos.write(bytes);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (descriptor != -1) python.closeFile(descriptor);
                if (bos != null) bos.close();
            } catch (IOException e) {
            }
        }

//        logger.debug("Downloading file=" + serverFile.getAbsolutePath() + ", length=" + clientFile.length());
//        logger.debug("Download completed in: " + (System.currentTimeMillis() - time) + " milliseconds");
    }

    public void delete() {
        clientFile.delete();
        if (!isLocal) python.deleteFile(serverFile);
    }
}

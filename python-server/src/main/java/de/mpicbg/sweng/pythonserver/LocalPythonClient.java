package de.mpicbg.sweng.pythonserver;

import java.io.File;
import java.io.IOException;


/**
 * Document me!
 *
 * @author Tom Haux
 */
public class LocalPythonClient implements Python {
    // PythonServer for local use
    PythonServer server = new LocalPythonServer();

    /**
     * PythonClient for local use
     */
    public LocalPythonClient() {
    }

    public File createTempFile(String prefix, String suffix) {
        return
        server.createTempFile(prefix, suffix);
    }

    public boolean deleteFile(File file) {
        return
        server.deleteFile(file);
    }

    public CommandOutput executeCommand(String[] command) {
        return
        server.executeCommand(command);
    }

    public int openFile(File file) throws IOException {
        return server.openFile(file);
    }

    public byte[] readFile(int descriptor) throws IOException {
        return server.readFile(descriptor);
    }

    public void writeFile(int descriptor, byte[] bytes) throws IOException {
        server.writeFile(descriptor, bytes);
    }

    public void closeFile(int descriptor) throws IOException {
        server.closeFile(descriptor);
    }
}

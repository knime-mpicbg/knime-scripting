package de.mpicbg.knime.scripting.python.srv;

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

    @Override
    public File createTempFile(String prefix, String suffix) {
        return server.createTempFile(prefix, suffix);
    }

    @Override
    public String getFilePath(File file) {
        return server.getFilePath(file);
    }

    @Override
    public boolean deleteFile(File file) {
        return server.deleteFile(file);
    }

    @Override
	public CommandOutput executeCommand(String[] command) {
		return server.executeCommand(command, true);
	}

    @Override
	public CommandOutput executeCommand(String[] command, boolean waitFor) {
        return server.executeCommand(command, waitFor);
    }

    @Override
    public byte[] readFile(int descriptor) throws IOException {
        return server.readFile(descriptor);
    }

    @Override
    public void writeFile(int descriptor, byte[] bytes) throws IOException {
        server.writeFile(descriptor, bytes);
    }

    @Override
    public void closeFile(int descriptor) throws IOException {
        server.closeFile(descriptor);
    }

	@Override
    public int openFile(File file) throws IOException {
        return server.openFile(file);
    }
}

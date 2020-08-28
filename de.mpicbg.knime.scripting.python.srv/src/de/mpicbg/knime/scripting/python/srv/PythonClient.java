package de.mpicbg.knime.scripting.python.srv;

import gnu.cajo.utils.extra.TransparentItemProxy;

import java.io.File;
import java.io.IOException;


/**
 * Document me!
 *
 * @author Tom Haux
 */
public class PythonClient implements Python {
    private Python python;

    /**
     * Create PythonClient that communicates with the PythonServer
     */
    public PythonClient() {
        this(Python.DEFAULT_HOST, Python.DEFAULT_PORT);
    }

    /**
     * Create PythonClient that communicates with the PythonServer
     */
    public PythonClient(String serverName, int serverPort) {
        try {
            String url = "//" +
                    serverName + ":" + serverPort + "/" + Python.REGISTRY_NAME;
            python =
                    (Python) TransparentItemProxy.getItem(url, new Class[]{Python.class});
        } catch (Throwable
                e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File createTempFile(String prefix, String suffix) {
        return python.createTempFile(prefix, suffix);
    }

    @Override
    public String getFilePath(File file) {
        return python.getFilePath(file);
    }

    @Override
    public boolean deleteFile(File file) {
        return python.deleteFile(file);
    }

    @Override
	public CommandOutput executeCommand(String[] command) {
		return executeCommand(command, true);
	}

    @Override
	public CommandOutput executeCommand(String[] command, boolean waitFor) {
        return python.executeCommand(command, waitFor);
    }

    @Override
    public int openFile(File file) throws IOException {
        return python.openFile(file);
    }

    @Override
    public byte[] readFile(int descriptor) throws IOException {
        return python.readFile(descriptor);
    }

    @Override
    public void writeFile(int descriptor, byte[] bytes) throws IOException {
        python.writeFile(descriptor, bytes);
    }

    @Override
    public void closeFile(int descriptor) throws IOException {
        python.closeFile(descriptor);
    }
}

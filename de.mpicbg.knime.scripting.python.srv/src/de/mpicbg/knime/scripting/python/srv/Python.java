package de.mpicbg.knime.scripting.python.srv;

import java.io.File;
import java.io.IOException;


/**
 * Remote interface to python. The used implmentation depends on the current context and is configurable by the user.
 *
 * @author Tom Haux
 */
public interface Python {
    public String REGISTRY_NAME = "PythonServer";
    public int DEFAULT_PORT = 1198;
    public String DEFAULT_HOST = "localhost";

    public File createTempFile(String prefix, String suffix);

    public String getFilePath(File file);

    public boolean deleteFile(File file);

    public CommandOutput executeCommand(String[] command, boolean waitFor);
    
    public CommandOutput executeCommand(String[] command);

    public int openFile(File file) throws IOException;

    public byte[] readFile(int descriptor) throws IOException;

    public void writeFile(int descriptor, byte[] bytes) throws IOException;

    public void closeFile(int descriptor) throws IOException;
}

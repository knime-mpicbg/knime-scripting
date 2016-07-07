package de.mpicbg.knime.scripting.python.srv;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.*;
import java.util.Arrays;
import java.util.Hashtable;


/**
 * A server implementation that exposes the Python-interface for remote clients.
 *
 * @author Holger Brandl
 */

public class PythonServer implements Python {
    private ServerFileMap map = new ServerFileMap();

    public PythonServer(int port) {
        try {
            System.out.println("Configuring on port: " + port);
            Remote.config(null, port, null, 0);
            System.out.println("Registering with name: " + REGISTRY_NAME);
            ItemServer.bind(this, REGISTRY_NAME);
            System.out.println("Python server started...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Construct a server for local use only--does not initialize the client/server framework
    protected PythonServer(boolean local) {
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

    public CommandOutput executeCommand(String[] command) {
        try {
            return exec(command);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Run an external command and return any output it generates.
     *
     * @param cmd
     * @return
     * @throws InterruptedException
     */
    private CommandOutput exec(String[] cmd) throws Exception {
        Process proc = null;
        proc = Runtime.getRuntime().exec(cmd);

        // Create threads to capture output on standard/error streams
        assert proc != null;
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

        // Kick them off
        errorGobbler.start();
        outputGobbler.start();

        // Wait for the command process to complete
        proc.waitFor();

        return new CommandOutput(outputGobbler, errorGobbler);
    }

    public int openFile(File file) throws IOException {
        return map.add(file);
    }

    ;

    public byte[] readFile(int descriptor) throws IOException {
        ServerFile file = map.get(descriptor);
        return file.read();
    }

    ;

    public void writeFile(int descriptor, byte[] bytes) throws IOException {
        ServerFile file = map.get(descriptor);
        file.write(bytes);
    }

    ;

    public void closeFile(int descriptor) throws IOException {
        ServerFile file = map.get(descriptor);
        file.close();
        map.remove(descriptor);
    }

    ;

    public static void main(String[] args) {
        if (args.length == 0) new PythonServer(DEFAULT_PORT);
        else {
            int port = Integer.parseInt(args[0]);
            new PythonServer(port);
        }
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

    /**
     * Basic open, read, write, close access for a file
     */
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
}

package de.mpicbg.knime.scripting.python.srv;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: haux
 * Date: 1/27/11
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class PythonUtilities {
    /**
     * Locate an executable on the system path.
     *
     * @param executableName
     * @return
     */
    public static File findExecutableOnPath(String executableName) {
        String systemPath = System.getenv("PATH");
        String[] pathDirs = systemPath.split(File.pathSeparator);

        File fullyQualifiedExecutable = null;
        for (String pathDir : pathDirs) {
            File file = new File(pathDir, executableName);
            if (file.isFile()) {
                fullyQualifiedExecutable = file;
                break;
            }
        }

        return fullyQualifiedExecutable;
    }


    /**
     * Return a byte array containing the bytes from
     *
     * @param file
     * @return
     * @throws IOException
     */
    public

    static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            throw new IOException("File exceeds the upload size limit");
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    public static void writeFileFromBytes(byte[] bytes, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);

        // Get the size of the file
        long length = bytes.length;

        // Write the file
        os.write(bytes);

        // Close the input stream and return bytes
        os.close();
        return;
    }
}

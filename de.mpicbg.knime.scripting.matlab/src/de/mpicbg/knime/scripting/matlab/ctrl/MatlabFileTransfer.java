package de.mpicbg.knime.scripting.matlab.ctrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import de.mpicbg.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;

/**
 * Encapsulates client and server temp-files to simplify create/delete/copy operations.
 * Since this wrapper can be used for local and remote MATLAB hosts, the temp-files
 * are created only upon request (getter methods).
 * 
 * TODO: create a read and write method and clean up; download, upload, writeStreamToFile and save method.
 * 
 * @author Tom Haux, Felix Meyenhofer
 */
public class MatlabFileTransfer {

	/** The files on client and server side */
	private File file;
	
	/** Prefix and suffix of the temp-files */
	private String prefix, suffix;
	
	/**
	 * Constructor for a for a file Transfer on the local machine only.
	 * This constructor only initializes the class without taking any
	 * implicit action.
	 * 
	 * @param prefix
	 * @param suffix
	 * @throws IOException
	 */
	public MatlabFileTransfer(String prefix, String suffix) throws IOException {
		this.prefix = prefix;
		this.suffix = suffix;
	}
	
	/**
	 * Constructor for a file in the local mode. We don't have a MATLAB server object
	 * 
	 * @param inputFile
	 */
	public MatlabFileTransfer(File inputFile) {
		this.prefix = FilenameUtils.getBaseName(inputFile.getAbsolutePath());
		this.suffix = "." + FilenameUtils.getExtension(inputFile.getAbsolutePath());
		this.file = inputFile;
	}
	
	/**
	 * Constructor for a transfer file where the resource will be streamed immediately
	 * to the local JVM temp-directory to be made available for MATLAB.
	 * 
	 * @param resourcePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public MatlabFileTransfer(String resourcePath) throws FileNotFoundException, IOException {
		this.prefix = FilenameUtils.getBaseName(resourcePath);
		this.suffix = "." + FilenameUtils.getExtension(resourcePath);
		file = new File(AbstractMatlabScriptingNodeModel.TEMP_PATH, FilenameUtils.getName(resourcePath));
		file.deleteOnExit();
		Bundle bundle = Platform.getBundle("de.mpicbg.knime.scripting.matlab");
		//URL resFile = bundle.getResource(resourcePath);
		URL resFile = bundle.getEntry("/resources/hashmaputils.m");
        InputStream resstream = resFile.openStream();
        writeStreamToFile(resstream, new FileOutputStream(file));
	}

	/**
	 * Get the client file object on the local machine.
	 * 
	 * @return Client file on local machine
	 */
	public File getFile() {
		if (file == null) {
			try {
				file = File.createTempFile(prefix, suffix);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("MATLAB file transfer: unable to create local file (client side).");
			}
			file.deleteOnExit();
		}
			
		return file;
	}

	/**
	 * Get the relative path of the clients temp-file
	 * 
	 * @return Relative path to the local temp-file
	 */
	public String getPath() {
		return getFile().getAbsolutePath();
	}

	/**
	 * Delete the transfer temp-file (on local and remote machine)
	 */
	public void delete() {
		if (file != null)
			file.delete();
	}
	
	/** Save an input stream to the client file
	 * 
	 * @param inputStream
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void save(InputStream inputStream) throws FileNotFoundException, IOException {
		writeStreamToFile(inputStream,  new FileOutputStream(getFile()));
	}
	
	
	 /**
     * Write an file input to an output stream.
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    private static void writeStreamToFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16384];
        while (true) {
            int count = in.read(buffer);
            if (count < 0)
                break;
            out.write(buffer, 0, count);
        }
        in.close();
        out.close();
    }	
}

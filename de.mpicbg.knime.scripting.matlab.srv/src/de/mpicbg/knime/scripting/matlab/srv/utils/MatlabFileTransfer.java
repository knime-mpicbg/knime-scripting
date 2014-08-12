package de.mpicbg.knime.scripting.matlab.srv.utils;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

import de.mpicbg.knime.scripting.matlab.srv.Matlab;
import de.mpicbg.knime.scripting.matlab.srv.MatlabRemote;


/**
 * Encapsulates client and server temp-files to simplify create/delete/copy operations.
 * Since this wrapper can be used for local and remote MATLAB hosts, the temp-files
 * are created only upon request (getter methods).
 * 
 * @author Tom Haux, Felix Meyenhofer
 */
public class MatlabFileTransfer {

	/** The files on client and server side */
	private File clientFile, serverFile;
	
	/** Prefix and suffix of the temp-files */
	private String prefix, suffix;
	
	/** Object to communicate with the server */
	private MatlabRemote server;

	
	/**
	 * Constructor for a transfer file (that exists on the server and the client side)
	 * in the JVM temp-directory by defining file prefix and suffix. Both files will
	 * just be initialized.
	 * 
	 * @param matlab {@link MatlabRemote} server object
	 * @param prefix Temp-file prefix
	 * @param suffix Temp-file suffix
	 * @throws IOException
	 */
	public MatlabFileTransfer(MatlabRemote matlab, String prefix, String suffix) throws IOException {
		this.server = matlab;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	/**
	 * Constructor for a transfer file that will be synchronized from the client to
	 * server immediately.
	 * This is deduced by the fact that we have a MATLAB server, and an existing file
	 * (that was created before presumably with content) so the next logical step is to
	 * make it available on the server. 
	 * 
	 * @param matlab MATLAB server object
	 * @param inputFile	Local temp-file
	 * @throws IOException
	 */
	public MatlabFileTransfer(MatlabRemote matlab, File inputFile) throws IOException {
		this(inputFile);
		this.server = matlab;
		upload();
	}
	
	/**
	 * Constructor for a file in the local mode. We don't have a MATLAB server object
	 * 
	 * @param inputFile
	 */
	public MatlabFileTransfer(File inputFile) {
		this.prefix = FilenameUtils.getBaseName(inputFile.getAbsolutePath());
		this.suffix = FilenameUtils.getExtension(inputFile.getAbsolutePath());
		this.clientFile = inputFile;
	}
	
	/**
	 * Constructor for a transfer file, where the resource from this package will 
	 * be streamed immediately to the server temp-file. 
	 * 
	 * @param matlab
	 * @param resourcePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public MatlabFileTransfer(MatlabRemote matlab, String resourcePath) throws FileNotFoundException, IOException {
		this(resourcePath);
		this.server = matlab;
		upload();
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
		this.suffix = FilenameUtils.getExtension(resourcePath);
		clientFile = new File(Matlab.TEMP_PATH, FilenameUtils.getName(resourcePath));
		clientFile.deleteOnExit();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream resstream = loader.getResourceAsStream(resourcePath);
        writeStreamToFile(resstream, new FileOutputStream(clientFile));
	}
	

	/**
	 * Get the client file object on the local machine.
	 * 
	 * @return Client file on local machine
	 */
	public File getClientFile() {
		if (clientFile == null) {
			try {
				clientFile = File.createTempFile(prefix, suffix);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("MATLAB file transfer: unable to create local file (client side).");
			}
			clientFile.deleteOnExit();
		}
			
		return clientFile;
	}

	/**
	 * Get the server temp-file on the remote machine.
	 * 
	 * @return Temp-file object of the server (on remote machine)
	 */
	private File getServerFile() {
		if (serverFile == null) {
			try {
				serverFile = server.createTempFile(prefix, suffix);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("MATLAB file transfer: unable to create remote file (server side).");
			}
		}
		return serverFile;
	}

	/**
	 * Get the relative path of the clients temp-file
	 * 
	 * @return Relative path to the local temp-file
	 */
	public String getClientPath() {
		return getClientFile().getAbsolutePath();
	}

	/**
	 * Get the relative path on the remote machine (server).
	 * 
	 * @return Relative path on the remote machine
	 */
	public String getServerPath() {
		return getServerFile().getAbsolutePath();
	}

	/**
	 * Upload the content from the local file (client) to the remote file (server)
	 */
	public void upload() { // TODO remove redundant code (I left it so fare to be sure we still have a working version)
		// Read the file into a byte array and pass it to the server
		int descriptor = -1;
		BufferedInputStream bis = null;

		try {
			byte[] buffer = new byte[8192];
			descriptor = server.openFile(getServerFile());
			bis = new BufferedInputStream(new FileInputStream(getClientFile()));
			int bytesRead = 0;

			// Keep reading until we hit the end of the file
			// when the end of the stream has been reached, -1 is returned
			while ((bytesRead = bis.read(buffer)) != -1) {
				byte[] b = buffer;
				if (bytesRead < buffer.length) b = Arrays.copyOf(buffer, bytesRead);
				server.writeFile(descriptor, b);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (descriptor != -1) 
					server.closeFile(descriptor);
				if (bis != null)
					bis.close();
			} catch (IOException e) {
				//TODO should we really ignore this?
			}
		}
	}
	
	
	public void upload(InputStream bis) throws IOException {
		// Read the file into a byte array and pass it to the server
		int descriptor = -1;


		try {
			byte[] buffer = new byte[8192];
			descriptor = server.openFile(getServerFile());
			bis = new BufferedInputStream(bis);
			int bytesRead = 0;

			// Keep reading until we hit the end of the file
			// when the end of the stream has been reached, -1 is returned
			while ((bytesRead = bis.read(buffer)) != -1) {
				byte[] b = buffer;
				if (bytesRead < buffer.length) b = Arrays.copyOf(buffer, bytesRead);
				server.writeFile(descriptor, b);
			}
		} catch (IOException e) {
			throw new RuntimeException("MATLAB file transfer: the upload was aborted.");
		} finally {
			if (descriptor != -1) 
				server.closeFile(descriptor);
			if (bis != null)
				bis.close();
		}
	}

	/**
	 * Fetch/download the content from the remote file (server) to the local file (server).
	 */
	public void download() {
		// Get a byte array from the server and write it to a file
		// Read the file into a byte array and pass it to the server
		int descriptor = -1;
		BufferedOutputStream bos = null;

		try {
			descriptor = server.openFile(getServerFile());

			bos = new BufferedOutputStream(new FileOutputStream(getClientFile()));
			while (true) {
				byte[] bytes = server.readFile(descriptor);
				if (bytes.length == 0) break;
				bos.write(bytes);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (descriptor != -1) 
					server.closeFile(descriptor);
				if (bos != null) 
					bos.close();
			} catch (IOException e) {
				//TODO should we really ignore this?
			}
		}
	}

	/**
	 * Delete the transfer temp-file (on local and remote machine)
	 */
	public void delete() {
		if (clientFile != null)
			clientFile.delete();
		if (serverFile != null)
			server.deleteFile(serverFile);
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

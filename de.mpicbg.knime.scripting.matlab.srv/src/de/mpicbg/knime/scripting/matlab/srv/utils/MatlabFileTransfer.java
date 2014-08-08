package de.mpicbg.knime.scripting.matlab.srv.utils;

import java.io.*;
import java.util.Arrays;

import de.mpicbg.knime.scripting.matlab.srv.MatlabRemote;


/**
 * Encapsulates client and server temp-files to simplify create/delete/copy operations
 * 
 * @author Tom Haux
 */
public class MatlabFileTransfer {

	/** The files on client and server side */
	private File clientFile, serverFile;
	
	/** Object to communicate with the server */
	private MatlabRemote server;

	
	/**
	 * Constructor for a transfer file (that exists on the server and the client side)
	 * in the JVM temp-directory by defining file prefix and suffix.
	 * 
	 * @param matlab MATLAB server object
	 * @param prefix Temp-file prefix
	 * @param suffix Temp-file suffix
	 * @throws IOException
	 */
	public MatlabFileTransfer(MatlabRemote matlab, String prefix, String suffix) throws IOException {
		this.server = matlab;

		clientFile = File.createTempFile(prefix, suffix);
		clientFile.deleteOnExit();
		serverFile = matlab.createTempFile(prefix, suffix);
	}

	/**
	 * Constructor for a transfer file (that exists on the server and the client side)
	 * in the JVM temp-directory by giving in the local {@link File} object.
	 * 
	 * @param matlab MATLAB server object
	 * @param inputFile	Local temp-file
	 * @throws IOException
	 */
	public MatlabFileTransfer(MatlabRemote matlab, File inputFile) throws IOException {
		this.server = matlab;

		clientFile = inputFile;
		serverFile = matlab.createTempFile("MatlabRemoteServer", ".tmp");
	}

	/**
	 * Get the client file object on the local machine.
	 * 
	 * @return Client file on local machine
	 */
	public File getClientFile() {
		return clientFile;
	}

	/**
	 * Get the server temp-file on the remote machine.
	 * 
	 * @return Temp-file object of the server (on remote machine)
	 */
	public File getServerFile() {
		return serverFile;
	}

	/**
	 * Get the relative path of the clients temp-file
	 * 
	 * @return Relative path to the local temp-file
	 */
	public String getClientPath() {
		return clientFile.getAbsolutePath();
	}

	/**
	 * Get the relative path on the remote machine (server).
	 * 
	 * @return Relative path on the remote machine
	 */
	public String getServerPath() {
		return server.getFilePath(serverFile);
	}

	/**
	 * Upload the content from the local file (client) to the remote file (server)
	 */
	public void upload() {
		// Read the file into a byte array and pass it to the server
		int descriptor = -1;
		BufferedInputStream bis = null;

		try {
			byte[] buffer = new byte[8192];
			descriptor = server.openFile(serverFile);
			bis = new BufferedInputStream(new FileInputStream(clientFile));
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

	/**
	 * Fetch/download the content from the remote file (server) to the local file (server).
	 */
	public void fetch() {
		// Get a byte array from the server and write it to a file
		// Read the file into a byte array and pass it to the server
		int descriptor = -1;
		BufferedOutputStream bos = null;

		try {
			descriptor = server.openFile(serverFile);

			bos = new BufferedOutputStream(new FileOutputStream(clientFile));
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
		clientFile.delete();
		server.deleteFile(serverFile);
	}
}

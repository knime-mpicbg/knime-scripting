package de.mpicbg.knime.scripting.matlab.srv;

import java.io.File;
import java.io.IOException;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabProxy;


/**
 * This interface defines the methods that can be accessed from another JVM 
 * (see cajo documentation).
 * Furthermore it contains some default settings for the MATLAB server
 * 
 * @author Holger Brandl, Tom Haux, Felix Meyenhofer
 */
public interface MatlabRemote {
	
	/** Default host */
	public static final String DEFAULT_HOST = "localhost";
	
	/** Default port the MATLAB server will listen to */
	public static final int DEFAULT_PORT = 1198;
	
	/** Registry name */
	public static final String REGISTRY_NAME = "MatlabServer";
	
	
	
	///////////////////////////// MATLAB access operations //////////////////////////// 
	
	/**
	 * Acquire a MATLAB proxy (via {@link MatlabController}) to get access
	 * to the MATLAB command line. Once a proxy is acquired, it is not accessible
	 * by anyone else (thread safe)
	 * 
	 * @return 
	 * @throws MatlabConnectionException
	 */
	public MatlabProxy acquireMatlabProxy() throws MatlabConnectionException;
	
	/**
	 * Release the MATLAB proxy and thus make it available for other processes again.
	 * 
	 * @param proxy
	 * @throws MatlabConnectionException
	 */
	public void releaseMatlabProxy(MatlabProxy proxy) throws MatlabConnectionException;
	
	
	/**
	 * Print message (provided by the client) on the servers standard output
	 * 
	 * @param msg
	 */
	public void printServerMessage(String msg);
	
	
	////////////////////// Server file operations (data transfer) ////////////////////// 
	
	/**
	 * Create a temp-file (JVM temp-directory) that can be manipulated and accessed
	 * on both, the client and the server (local machine and remote host)
	 * 
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException
	 */
	public File createTempFile(String prefix, String suffix) throws IOException;

	
	/**
	 * Get the relative file path
	 * 
	 * @param file
	 * @return
	 */
	public String getFilePath(File file);

	/**
	 * Delete the temp-file
	 * 
	 * @param file
	 * @return
	 */
	public boolean deleteFile(File file);

	/**
	 * Open the temp-file
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public int openFile(File file) throws IOException;

	/**
	 * Read the temp-file
	 * 
	 * @param descriptor
	 * @return
	 * @throws IOException
	 */
	public byte[] readFile(int descriptor) throws IOException;

	/**
	 * Write the temp-file
	 * 
	 * @param descriptor
	 * @param bytes
	 * @throws IOException
	 */
	public void writeFile(int descriptor, byte[] bytes) throws IOException;

	/**
	 * Close the temp-file
	 * 
	 * @param descriptor
	 * @throws IOException
	 */
	public void closeFile(int descriptor) throws IOException;

}


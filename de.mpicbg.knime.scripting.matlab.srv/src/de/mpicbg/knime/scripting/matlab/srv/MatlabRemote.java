package de.mpicbg.knime.scripting.matlab.srv;

import java.io.File;
import java.io.IOException;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;


/**
 * 
 * @author Felix Meyenhofer
 */
public interface MatlabRemote {
	
	/** Default host */
	public static final String DEFAULT_HOST = "localhost";
	
	/** Default port the MATLAB server will listen to */
	public static final int DEFAULT_PORT = 1198;
	
	/** Registry name */
	public static final String REGISTRY_NAME = "MatlabServer";
	
	
	
	public MatlabProxy acquireMatlabProxy() throws MatlabConnectionException;
	
	public void releaseMatlabProxy(MatlabProxy proxy) throws MatlabConnectionException;
	
//	public void eval(String cms) throws MatlabInvocationException;
//	
//	public Object getVariable(String var) throws MatlabInvocationException;
	
	
	
	public File createTempFile(String prefix, String suffix) throws IOException;

	public String getFilePath(File file);

	public boolean deleteFile(File file);

	public int openFile(File file) throws IOException;

	public byte[] readFile(int descriptor) throws IOException;

	public void writeFile(int descriptor, byte[] bytes) throws IOException;

	public void closeFile(int descriptor) throws IOException;
	
	

}

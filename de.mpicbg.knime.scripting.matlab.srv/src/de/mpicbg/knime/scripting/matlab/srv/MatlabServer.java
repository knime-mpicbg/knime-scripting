package de.mpicbg.knime.scripting.matlab.srv;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;



/**
 * 
 * @author Holger Brandl, Tom Haux, Felix Meyenhofer
 */
public class MatlabServer implements MatlabRemote {
	
	/** Hash to maintain the file map */
	private ServerFileMap fileMap;
	
	/** MATLAB controller object */
	private MatlabController matlabController;
	
	/** MATLAB proxy holder */
	private ArrayList<MatlabProxy> matlabProxyHolder = new ArrayList<MatlabProxy>(1);
	
	
	public MatlabServer(int port) {
		try {
            System.out.println("Configuring on port: " + port);
            Remote.config(null, port, null, 0);
            
            System.out.println("Registering with name: " + REGISTRY_NAME);
            ItemServer.bind(this, REGISTRY_NAME);
            
            System.out.println("Starting MATLAB application...");
            matlabController = new MatlabController();
        } catch (IOException e) {
        	System.err.println("Unable to write registry.");
            e.printStackTrace();
        } catch (MatlabConnectionException e) {
			System.err.println("Unable to connect to the MATLAB applicatoin.");
			e.printStackTrace();
		}
	}


	@Override
	public MatlabProxy acquireMatlabProxy() throws MatlabConnectionException {
		MatlabProxy proxy = matlabController.acquireProxyFromQueue();
		matlabProxyHolder.add(proxy);
		System.out.println("MATLAB Proxy acquired.");
		return proxy;
	}


	@Override
	public void releaseMatlabProxy(MatlabProxy proxy) {
		if (proxy == null) {
			if (this.matlabProxyHolder.size() > 0) {
				this.matlabController.returnProxyToQueue(this.matlabProxyHolder.remove(1));
				System.out.println("Proxy released.");
			} else {
				System.out.println("Oups, we lost a proxy along the way. Time for serious debugging.");
			}
		} else {
			this.matlabController.returnProxyToQueue(proxy);
			this.matlabProxyHolder.remove(proxy);
			System.out.println("The proxy released");
		}
	}


//	@Override
//	public void eval(String cmd) throws MatlabInvocationException {
//		if (matlabProxyHolder.size() == 0)
//			throw new RuntimeException("Currently there is no proxy at disposel. ");
//		
//		MatlabProxy proxy = matlabProxyHolder.get(1);
//		proxy.eval(cmd);
//	}
//
//
//	@Override
//	public Object getVariable(String var) throws MatlabInvocationException {
//		if (matlabProxyHolder.size() == 0)
//			throw new RuntimeException("Currently there is no proxy at disposel. ");
//		
//		MatlabProxy proxy = matlabProxyHolder.get(0);
//		return proxy.getVariable(var);
//	}
	
	
	
	
	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        return tempFile;
    }
	
	@Override
	public String getFilePath(File file) {
        return file.getAbsolutePath();
    }

	@Override
    public boolean deleteFile(File file) {
        return file != null ? file.delete() : true;
    }

	@Override
    public int openFile(File file) throws IOException {
        return fileMap.add(file);
    }

	@Override
    public byte[] readFile(int descriptor) throws IOException {
        ServerFile file = fileMap.get(descriptor);
        return file.read();
    }

	@Override
    public void writeFile(int descriptor, byte[] bytes) throws IOException {
        ServerFile file = fileMap.get(descriptor);
        file.write(bytes);
    }

	@Override
    public void closeFile(int descriptor) throws IOException {
        ServerFile file = fileMap.get(descriptor);
        file.close();
        fileMap.remove(descriptor);
    }

	

    /**
     * Maintain a map of descriptors to ServerFile references.
     */
    class ServerFileMap {

    	/** Current position in the file maps */
        private int currentDescriptor = 0;
        
        /** File map */
        private Hashtable<String, ServerFile> map = new Hashtable<String, ServerFile>();


        /**
         * Constructor of the file map
         * 
         * @param descriptor
         * @return
         */
        public ServerFile get(int descriptor) {
            return map.get(Integer.toString(descriptor));
        }

        /**
         * Add a file to the map
         * 
         * @param file
         * @return
         */
        public int add(File file) {
            return add(new ServerFile(file));
        }

        /**
         * Add a file to the map
         * 
         * @param file
         * @return
         */
        public int add(ServerFile file) {
            String descriptor = Integer.toString(currentDescriptor);
            map.put(descriptor, file);
            return currentDescriptor++;
        }

        /**
         * Remove a file form the map
         * 
         * @param descriptor
         */
        public void remove(int descriptor) {
            map.remove(Integer.toString(descriptor));
        }
    }


    /**
     * Wrapper for the File object with write and read methods
     */
    class ServerFile {

        /** This stream or the {@link ServerFile#output} will be created */
        private InputStream input;
        
        /** This OR the {@link ServerFile#input} */
        private OutputStream output;
        
        /** Buffer used for read operations */
        private byte[] buffer = new byte[8192];
        
        /** File object */
        private File file;


        /**
         * Constructor of the ServerFile
         * 
         * @param file
         */
        public ServerFile(File file) {
            this.file = file;
        }


        /**
         * Read the server file
         * 
         * @return
         * @throws IOException
         */
        public byte[] read() throws IOException {
            // Create the stream if this is the first file operation
            if (input == null) input = new BufferedInputStream(new FileInputStream(file));

            int n = input.read(buffer);
            byte[] b = buffer;
            if (n == -1) b = new byte[0];
            else if (n < buffer.length) b = Arrays.copyOf(buffer, n);

            return b;
        }

        /**
         * Write the server file
         * 
         * @param bytes
         * @throws IOException
         */
        public void write(byte[] bytes) throws IOException {
            // Create the stream if this is the first file operation
            if (output == null) output = new BufferedOutputStream(new FileOutputStream(file));
            output.write(bytes);
        }

        /**
         * Close the server file
         * 
         * @throws IOException
         */
        public void close() throws IOException {
            // Close whichever stream was created
            if (input != null) input.close();
            if (output != null) output.close();
        }
    }

}

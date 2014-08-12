package de.mpicbg.knime.scripting.matlab.srv;

import gnu.cajo.utils.extra.TransparentItemProxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.net.InetAddress;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

import de.mpicbg.knime.scripting.matlab.srv.utils.MatlabCode;
import de.mpicbg.knime.scripting.matlab.srv.utils.MatlabFileTransfer;
import de.mpicbg.knime.scripting.matlab.srv.utils.MatlabTable;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;


/**
 * This MATLAB client uses the {@link Matlab} interface to expose 
 * the needed functions to the KNIME nodes. The node does not have to care
 * if the MATLAB code is executed locally or remotely. In the first case 
 * it uses the {@link MatlabController} to start and process the MATLAB
 * commands, in the second case it uses the cajo library to communicate
 * with the JVM on a remote machine that works like a MATLAB controller.
 * 
 * @author Felix Meyenhofer
 */
public class MatlabClient {
	
	/** MATLAB client object (can either use a remote or a local MATLAB session) */
	public final Matlab client;
	
	/** Store the local/remote flag for information purposes */
	public final boolean local;
	
	/** Data transfer method */
	public int method;
	
	/** Number of MATLAB application instance */
	public int sessions;
	
	/** Thread number (for identification during debugging) */
	private int clientNumber;
	
	/** Total count of threads connecting to MATLAB */
	static Integer clientCount;
	
	
	/**
	 * Constructor of the MATLAB client.
	 * It uses the local flag to determine weather to use a local
	 * MATLAB session or to communicate with a remote session on another
	 * machine.
	 * 
	 * @param local
	 * @throws MatlabConnectionException
	 */
	public MatlabClient(boolean local, int sessions) throws MatlabConnectionException {
		this(local, sessions, MatlabRemote.DEFAULT_HOST, MatlabRemote.DEFAULT_PORT);
	} 
	
	
	/**
	 * Constructor of the MATLAB client.
	 * The additional parameters allow to access a non-default host running
	 * a {@link MatlabServer}
	 * 
	 * @param local
	 * @param host
	 * @param port
	 * @throws MatlabConnectionException
	 */
	public MatlabClient(boolean local, int sessions, String host, int port) throws MatlabConnectionException {
		this.local = local;
		
		// Determine the total number of threads and the number of this thread
		if (clientCount == null) {
			clientCount = 1;
		} else {
			clientCount++;
		}
		this.clientNumber = clientCount;

		// Instantiate the MATLAB client
		if (local) {
			client = new Local(sessions, this.clientNumber);
		} else {
			client = new Remote(host, port, this.clientNumber);
		}		
	}
	
	/**
	 * Wrapper of the corresponding method in {@link Matlab} for easier access
	 * 
	 * @throws InterruptedException
	 * @throws MatlabConnectionException 
	 */
	public void rollback() throws InterruptedException, MatlabConnectionException {
		this.client.rollback();
	}
	
	/**
	 * Wrapper of the corresponding method in {@link Matlab} for easier access
	 */
	public void cleanup() {
		this.client.cleanup();
	}
	
	
	
	
	/**
	 * Implementation of a local MATLAB client
	 */
	private class Local implements Matlab {
		
		/** MATLAB controller object */
		private MatlabController matlabController;
		
		/** Proxy holder, that allows to access the proxy for exception handling */
		private ArrayList<MatlabProxy> matlabProxyHolder = new ArrayList<MatlabProxy>(1);
		
		/** Object to generate the MATLAB code needed for a particular task */ 
		private MatlabCode code;
		
		/** Object to hold the KNIME table and allowing MATLAB compatible transformations */
		private MatlabTable table;
		
		/** Client number (to distinguish the calls to {@link MatlabController} and {@link MatlabServer} */
		private int clientNumber;
	
		
		/**
		 * Constructor of the local MATLAB client.
		 * This initializes the MatlabController.
		 * 
		 * @throws MatlabConnectionException
		 */
		public Local(int sessions, int clientNumber) throws MatlabConnectionException {
			this.clientNumber = clientNumber;
			this.matlabController = new MatlabController(sessions);
			System.out.println("Created local MATLAB client " + this.clientNumber);
		}

		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void openTask(BufferedDataTable inputTable, String matlabType, String transferMethod) 
				throws Exception {
	       
			MatlabProxy proxy;
			
			// Transfer the KNIME table as hash map object dump to the JVM temp-folder
			table = new MatlabTable(inputTable);
			String cmd;
			if (transferMethod.equals("file")) {
		        table.writeHashMapToTempFolder();
		        code = new MatlabCode(table.getTempFile(), matlabType);
		        cmd = code.prepareOpenCode(false);
		        proxy = acquireMatlabProxy();
			} else if (transferMethod.equals("workspace")){
				code = new MatlabCode(matlabType);
				proxy = acquireMatlabProxy();
				table.pushTable2MatlabWorkspace(proxy, matlabType);
				cmd = code.prepareOpenCode(false);
			} else {
				throw new RuntimeException("Unknown method: " + transferMethod);
			}
			
			// Execute 
	        proxy.eval(cmd);
	        releaseMatlabProxy(proxy);
	        
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public BufferedDataTable snippetTask(BufferedDataTable inputTable, String transferMethod, ExecutionContext exec, String snippet, String matlabType)
				throws Exception {
			
			if (transferMethod.equals("file")) {
				// Convert the KNIME table and write it to the temp-directory
				table = new MatlabTable(inputTable);
				table.writeHashMapToTempFolder();

				// Add the MATLAB code to the snippet and transfer the scripts to the temp-directory
				code = new MatlabCode(snippet, table.getTempFile(), matlabType);
				String cmd = code.prepareSnippetCode(false);			

				// Run it in MATLAB
				MatlabProxy proxy = acquireMatlabProxy();
				proxy.eval(cmd);
				MatlabCode.checkForSnippetErrors(proxy);
				proxy.eval("disp('exectuted snippet and updated " + Matlab.OUTPUT_VARIABLE_NAME + "')");
				releaseMatlabProxy(proxy);

				// Get the data back
				table.readHashMapFromTempFolder(exec);
				return table.getBufferedDataTable();
				
			} else if (transferMethod.equals("workspace")) {
				// Get a proxy (block it)
				MatlabProxy proxy = acquireMatlabProxy();

				// Convert the KNIME table and write it to the temp-directory
				table = new MatlabTable(inputTable);

				// Push the table to the input variable in the MATLAB workspace.
				table.pushTable2MatlabWorkspace(proxy, matlabType);
				
				// Create a script from the snippet
				code = new MatlabCode(snippet, matlabType);
				String cmd = code.prepareSnippetCode(true);		

				// Run the snippet it in MATLAB
				proxy.eval(cmd);
				MatlabCode.checkForSnippetErrors(proxy);
				proxy.eval("disp('exectuted snippet and updated " + Matlab.INPUT_VARIABLE_NAME + ", " + Matlab.OUTPUT_VARIABLE_NAME + ", " + Matlab.COLUMNS_VARIABLE_NAME + "')");

				// Pull the data from the output variable in the MATLAB workspace
				BufferedDataTable outputTable = table.pullTableFromMatlabWorkspace(exec, proxy, matlabType);

				// Return the proxy
				releaseMatlabProxy(proxy);

				return outputTable;
			} else {
				return null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public File plotTask(BufferedDataTable inputTable, String transferMethod, String snippet, Integer plotWidth, Integer plotHeight, String matlabType) 
				throws Exception {

			if (transferMethod.equals("file")) {
				// Transfer the KNIME table as hash map object dump to the JVM temp-folder
				table = new MatlabTable(inputTable);
				table.writeHashMapToTempFolder();
				
				// Copy the MATLAB script to the temp-directory and get the file name with the random string in it
				code = new MatlabCode(snippet, table.getTempFile(), matlabType);
				String cmd = code.preparePlotCode(plotWidth, plotHeight, false);
				
				// Execute 
				MatlabProxy proxy = acquireMatlabProxy();
				proxy.eval(cmd);
				MatlabCode.checkForSnippetErrors(proxy);
				proxy.eval("disp('created plot.')");
			    releaseMatlabProxy(proxy);

			    // Return the png-image
				return code.getPlotFile();
			} else if (transferMethod.equals("workspace")) {
				// Get a proxy (block it)
				MatlabProxy proxy = acquireMatlabProxy();

				// Push the table to the input variable in the MATLAB workspace.
				table = new MatlabTable(inputTable);
				table.pushTable2MatlabWorkspace(proxy, matlabType);

				// Create a server file for the output plot-image
				code = new MatlabCode(snippet, matlabType);
				String cmd = code.preparePlotCode(plotWidth, plotHeight, true);
				
				// Run the snippet it in MATLAB
				proxy.eval(cmd);
				MatlabCode.checkForSnippetErrors(proxy);
				proxy.eval("disp('created plot and updated " + Matlab.INPUT_VARIABLE_NAME + ", " + Matlab.COLUMNS_VARIABLE_NAME + " ')");
				
				// Release the proxy
				releaseMatlabProxy(proxy);
				
				return code.getPlotFile();
				
			} else {
				return null;
			}
		}

		/**
		 * {@inheritDoc} 
		 */
		@Override
		public void rollback() throws InterruptedException {
			if (this.matlabProxyHolder.size() > 0) {
				MatlabProxy proxy = this.matlabProxyHolder.remove(0);
				if (proxy != null) {
					this.matlabController.returnProxyToQueue(proxy);
					System.out.println("Emergency proxy return");
				} else {
					System.out.println("No proxy to return");
				}
			} else {
				System.out.println("Proxy already returned.");
			}
		}

		/**
		 * {@inheritDoc} 
		 */
		@Override
		public void cleanup() {
			if (this.code != null)
				this.code.cleanup();
			if (this.table != null)
				this.table.cleanup();
		}
		
		/**
		 * This method get's the controller for the MATLAB application
		 * and keeps it accessible in a field in case of an interruption
		 * 
		 * @return {@link MatlabProxy}
		 * @throws MatlabInvocationException
		 * @throws MatlabConnectionException 
		 */
		private MatlabProxy acquireMatlabProxy() throws MatlabInvocationException, MatlabConnectionException {
			MatlabProxy proxy = matlabController.acquireProxyFromQueue();
			matlabProxyHolder.add(proxy);
			proxy.eval(MatlabCode.getThreadInforCommand(matlabController.getThreadNumber()));
	        return proxy;
		}
		
		/**
		 * This method returns the MATLAB proxy and removes it from the field.
		 *  
		 * @param proxy
		 */
		private void releaseMatlabProxy(MatlabProxy proxy) {
			matlabController.returnProxyToQueue(proxy);
			matlabProxyHolder.remove(proxy);
		}
		
	}
	
	
	
	
	/**
	 * Implementation of the client talking to a remote MATLAB session.
	 */
	private class Remote implements Matlab, MatlabRemote {
		
		/** MATLAB Server object */
		private MatlabRemote matlabServer;
		
		/** Object to generate the MATLAB code needed for a particular task */ 
		private MatlabCode code;
		
		/** Object to hold the KNIME table and allowing MATLAB compatible transformations */
		private MatlabTable table;
		
		/** Temp-file containing the plot image */
		private MatlabFileTransfer plot;
		
		/** Client number (to distinguish the calls to {@link MatlabController} and {@link MatlabServer} */
		private int clientNumber;

		/**
		 * Constructor
		 */
		public Remote(String serverName, int serverPort, int clientNumber) {
			this.clientNumber = clientNumber;
			
	        try {
	            String url = "//" + serverName + ":" + serverPort + "/" + MatlabRemote.REGISTRY_NAME;
	            matlabServer = (MatlabRemote) TransparentItemProxy.getItem(url, new Class[]{MatlabRemote.class});
	            System.out.println("Created remote MATLAB client " + this.clientNumber + "(server: )" + url);	            
	            matlabServer.printServerMessage("Connection from " + InetAddress.getLocalHost().getHostName() + ", client " + this.clientNumber);
	        } catch (Throwable e) {
	        	System.err.println("Unable to connect to MATLAB server.");
	            throw new RuntimeException(e);
	        }
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void openTask(BufferedDataTable inputTable, String matlabType, String transferMethod)
				throws IOException, MatlabInvocationException {
			
			// This task is not used for a remote client. It only makes sense if you have the MATLAB application on the same machine.
			throw new RuntimeException("You are about to try to push your data to the MATLAB workspace " +
					"on the server, where you probably can't see it.");
		}
	
		/**
		 * {@inheritDoc}
		 */
		@Override
		public BufferedDataTable snippetTask(BufferedDataTable inputTable, String transferMethod, ExecutionContext exec, String snippet, String matlabType)
				throws Exception {

			// Get a proxy (block it)
			MatlabProxy proxy = matlabServer.acquireMatlabProxy();
			
			// Convert the KNIME table and write it to the temp-directory
			table = new MatlabTable(inputTable);

			// Push the table to the input variable in the MATLAB workspace.
			table.pushTable2MatlabWorkspace(proxy, matlabType);
				
			// Run the snippet it in MATLAB
			proxy.eval(snippet);

			// Pull the data from the output variable in the MATLAB workspace
			BufferedDataTable outputTable = table.pullTableFromMatlabWorkspace(exec, proxy, matlabType);
		
			// Return the proxy
			matlabServer.releaseMatlabProxy(proxy);
			
			return outputTable;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public File plotTask(BufferedDataTable inputTable, String transferMethod, String snippet, Integer plotWidth, Integer plotHeight, String matlabType) 
				throws IOException, Exception {
			// Get a proxy (block it)
			MatlabProxy proxy = matlabServer.acquireMatlabProxy();

			// Push the table to the input variable in the MATLAB workspace.
			table = new MatlabTable(inputTable);
			table.pushTable2MatlabWorkspace(proxy, matlabType);

			// Create a server file for the output plot-image
			plot = new MatlabFileTransfer(matlabServer, Matlab.PLOT_TEMP_FILE_PREFIX, Matlab.PLOT_TEMP_FILE_SUFFIX);
			
			// Add the code to produce a png-file from the plot
			MatlabCode code = new MatlabCode(plot.getServerFile(), snippet);
			String cmd = code.addPlotCode(plotWidth, plotHeight);
			
			// Run the snippet it in MATLAB
			proxy.eval(cmd);
			
			// Release the proxy
			matlabServer.releaseMatlabProxy(proxy);
			
			// Fetch the output from the server
			plot.fetch();
			
			return plot.getClientFile();
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void rollback() throws MatlabConnectionException {
			releaseMatlabProxy(null);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void cleanup() {
			plot.delete();
			code.cleanup();
			table.cleanup();
		}
		
		/**
		 * {@inheritDoc} 
		 */
		@Override
		public MatlabProxy acquireMatlabProxy() throws MatlabConnectionException {
			return matlabServer.acquireMatlabProxy();
		}
		
		/**
		 * {@inheritDoc} 
		 */
		@Override
		public void releaseMatlabProxy(MatlabProxy proxy) throws MatlabConnectionException {
			matlabServer.releaseMatlabProxy(proxy);
		}
		
		/**
		 * {@inheritDoc}  
		 */
		@Override
		public File createTempFile(String prefix, String suffix) throws IOException {
			return matlabServer.createTempFile(prefix, suffix);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public String getFilePath(File file) {
			return matlabServer.getFilePath(file);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public boolean deleteFile(File file) {
			return matlabServer.deleteFile(file);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public int openFile(File file) throws IOException {
			return matlabServer.openFile(file);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public byte[] readFile(int descriptor) throws IOException {
			 return matlabServer.readFile(descriptor);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void writeFile(int descriptor, byte[] bytes) throws IOException {
			matlabServer.writeFile(descriptor, bytes);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void closeFile(int descriptor) throws IOException {
			matlabServer.closeFile(descriptor);
		}


		@Override
		public void printServerMessage(String msg) {
			matlabServer.printServerMessage(msg);
		}
		
	}

	
}


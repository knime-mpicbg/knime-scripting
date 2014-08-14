package de.mpicbg.knime.scripting.matlab.srv;

import gnu.cajo.utils.extra.TransparentItemProxy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
	public Matlab client;
	
	/** Store the local/remote flag for information purposes */
	public final boolean local;
	
	/** Data transfer method */
	public int method;
	
	/** Number of MATLAB application instance */
	public int sessions;
	
	/** Thread number (for identification during debugging) */
	private int clientNumber;

	/** Host name that provides the MATLAB application access */
	private String host;

	/** Port where the MATLAB host is listening */
	private int port;

	/** Timeout in milliseconds when trying to connect to a remote MATLAB host */
	private final long timeout = 1500;
	
	/** Total count of clients (nodes) on the local machine connecting to MATLAB */
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
			this.host = host;
			this.port = port;
			
			// Use an executor to impose a connection time out.
			ExecutorService executor = Executors.newCachedThreadPool();
			Callable<Object> task = new Callable<Object>() {
			   public Object call() {
			      return getRemote();
			   }
			};
			Future<Object> future = executor.submit(task);
			try {
			   client = (Remote)future.get(this.timeout , TimeUnit.MILLISECONDS); 
			} catch (Exception e) {
				future.cancel(true);
				System.out.println("The operation timed out.");
				e.printStackTrace();
			} finally {
				if (client == null)
					throw new RuntimeException("The connection to the MATLAB server timed out. " +
							"Either the server is not running, or it was not able to checkout a MATLAB license");
//					client = new Local(sessions, this.clientNumber);
			}
//			client = new Remote(host, port, this.clientNumber);
		}		
	}
	
	/**
	 * Getter for the Remote object. This is just to avoid having to deal
	 * with inputs in the task object that later is handed to the executor.
	 * 
	 * @return
	 */
	private Remote getRemote() {
		 return new Remote(host, port, this.clientNumber);
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
		
		/** Temp-file holding the plot image */
		private MatlabFileTransfer plotFile;
		
		/** Temp-file holding the hashmaputils.m for data parsing in MATLAB */
		private MatlabFileTransfer parserFile;
		
		/** Temp-file holding the snippet code */
		private MatlabFileTransfer codeFile;
		
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
			this.matlabController = new MatlabController(sessions, false);
			System.out.println("Created local MATLAB client " + this.clientNumber);
		}

		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void openTask(BufferedDataTable inputTable, String matlabType, String transferMethod) 
				throws Exception {

			MatlabProxy proxy;
			table = new MatlabTable(inputTable);
			
			if (transferMethod.equals("file")) {
				// Transfer the KNIME table as hash map object dump to the JVM temp-folder
		        table.writeHashMapToTempFolder();
		        
		        // Prepare the MATLAB parser script
		        parserFile = new MatlabFileTransfer(Matlab.MATLAB_HASHMAP_SCRIPT);
		        
		        // Compile the command to open the data in MATLAB
		        String cmd = MatlabCode.getOpenInMatlabCommand(matlabType, parserFile.getClientPath(), table.getHashMapTempPath());
		        
		        // Execute
		        proxy = acquireMatlabProxy();
		        proxy.eval(cmd);
		        releaseMatlabProxy(proxy);
		        
			} else if (transferMethod.equals("workspace")){
				proxy = acquireMatlabProxy();
				table.pushTable2MatlabWorkspace(proxy, matlabType);
				proxy.eval(MatlabCode.getOpenMessage(matlabType));
				releaseMatlabProxy(proxy);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public BufferedDataTable snippetTask(BufferedDataTable inputTable, String transferMethod, ExecutionContext exec, String snippet, String matlabType)
				throws Exception {
			
			// Prepare snippet temp-file
			codeFile = new MatlabFileTransfer(Matlab.SNIPPET_TEMP_FILE_PREFIX, Matlab.SNIPPET_TEMP_FILE_SUFFIX);
			
			table = new MatlabTable(inputTable);
			
			if (transferMethod.equals("file")) {
				// Convert the KNIME table and write it to the temp-directory
				table.writeHashMapToTempFolder();
				
				// Prepare the MATLAB parser script
		        parserFile = new MatlabFileTransfer(Matlab.MATLAB_HASHMAP_SCRIPT);

				// Add the MATLAB code to the snippet and transfer the scripts to the temp-directory
				code = new MatlabCode(snippet, matlabType, 
						parserFile.getClientPath(), 
						codeFile.getClientPath(), 
						table.getHashMapTempPath());
				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
		        String cmd = code.getScriptExecutionCommand(codeFile.getClientPath(), false, true);		

				// Run it in MATLAB
				MatlabProxy proxy = acquireMatlabProxy();
				proxy.eval(cmd);
				MatlabCode.checkForScriptErrors(proxy);
				proxy.eval(MatlabCode.getSnippetNodeMessage(false));
				releaseMatlabProxy(proxy);

				// Get the data back
				table.readHashMapFromTempFolder(exec);
				return table.getBufferedDataTable();
				
			} else if (transferMethod.equals("workspace")) {
				// Create a script from the snippet
				code = new MatlabCode(snippet, matlabType, 
						codeFile.getClientPath());
				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getClientPath(), true, true);
				
				// Get a proxy (block it) push the data execute the snippet and pull back the modified data
				MatlabProxy proxy = acquireMatlabProxy();
				table.pushTable2MatlabWorkspace(proxy, matlabType);
				proxy.eval(cmd);
				MatlabCode.checkForScriptErrors(proxy);
				proxy.eval(MatlabCode.getSnippetNodeMessage(true)); //TODO pack this in a function in matlabCode.
				BufferedDataTable outputTable = table.pullTableFromMatlabWorkspace(exec, proxy, matlabType);
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

			// Prepare snippet temp-file
			codeFile = new MatlabFileTransfer(Matlab.SNIPPET_TEMP_FILE_PREFIX, Matlab.SNIPPET_TEMP_FILE_SUFFIX);
			// Prepare the plot temp file
			plotFile = new MatlabFileTransfer(Matlab.PLOT_TEMP_FILE_PREFIX, Matlab.PLOT_TEMP_FILE_SUFFIX);
			// Get the  input table data ready for processing
			table = new MatlabTable(inputTable);
			
			if (transferMethod.equals("file")) {
				// Transfer the KNIME table as hash map object dump to the JVM temp-folder
				table.writeHashMapToTempFolder();
				
				// Prepare the parser temp-file
				parserFile = new MatlabFileTransfer(Matlab.MATLAB_HASHMAP_SCRIPT);
				
				// Prepare the code snippet
				code = new MatlabCode(snippet, matlabType, 
						parserFile.getClientPath(), 
						codeFile.getClientPath(), 
						table.getHashMapTempPath(), 
						plotFile.getClientPath(),
						plotWidth, plotHeight);
				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getClientPath(), false, false);
				
				// Execute
				MatlabProxy proxy = acquireMatlabProxy();
				proxy.eval(cmd);
				MatlabCode.checkForScriptErrors(proxy);
				proxy.eval(MatlabCode.getPlotNodeMessage(false));
			    releaseMatlabProxy(proxy);
				
			} else if (transferMethod.equals("workspace")) {
				// Get a proxy (block it)
				MatlabProxy proxy = acquireMatlabProxy();

				// Push the table to the input variable in the MATLAB workspace.
				table.pushTable2MatlabWorkspace(proxy, matlabType);
				
				// Prepare the code snippet.
				code = new MatlabCode(snippet, matlabType,
						codeFile.getClientPath(), 
						plotFile.getClientPath(), 
						plotWidth, plotHeight);
				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getClientPath(), true, false);
				
				// Execute 
				proxy.eval(cmd);
				MatlabCode.checkForScriptErrors(proxy);
				proxy.eval(MatlabCode.getPlotNodeMessage(false));
			    releaseMatlabProxy(proxy);
			} 

		    // Return the png-image
			return plotFile.getClientFile();
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
			if (this.table != null)
				this.table.cleanup();
			if (codeFile !=null)
				codeFile.delete();
			if (parserFile != null)
				parserFile.delete();
			if (plotFile != null)
				plotFile.delete();
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
			if (matlabProxyHolder.size() == 0) {
				MatlabProxy proxy = matlabController.acquireProxyFromQueue();
				matlabProxyHolder.add(proxy);
				proxy.eval(MatlabCode.getThreadInfoCommand(matlabController.getThreadNumber()));
				proxy.eval(MatlabCode.getClearWorkspaceCommand());
		        return proxy;
			} else {
				return matlabProxyHolder.get(0);
			}
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
		
		/** Temp-file containing the table data */
		private MatlabFileTransfer tableFile;
		
		/** Temp-file containing the data parser script */
		private MatlabFileTransfer parserFile;
		
		/** Temp-file containing the plot image */
		private MatlabFileTransfer plotFile;
		
		/** Temp-file containing the matlab code (script) */
		private MatlabFileTransfer codeFile;
		
		/** Client number (to distinguish the calls to {@link MatlabController} and {@link MatlabServer} */
		private int clientNumber;
		
		private String hostName;


		/**
		 * Constructor
		 */
		public Remote(String serverName, int serverPort, int clientNumber) {
			this.clientNumber = clientNumber;
			try {
				this.hostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e1) {
				this.hostName = "Unknown host";
			}
			
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
			
			// Get the data ready for conversion
			table = new MatlabTable(inputTable);
			
			// Prepare the MATLAB code
			codeFile = new MatlabFileTransfer(matlabServer, Matlab.SNIPPET_TEMP_FILE_PREFIX, Matlab.SNIPPET_TEMP_FILE_SUFFIX);
			
			if (transferMethod.equals("file")) {
				// Transfer the KNIME table to the server
				table.knimeTable2LinkedHashMap();
				tableFile = new MatlabFileTransfer(matlabServer, Matlab.TABLE_TEMP_FILE_PREFIX, Matlab.TABLE_TEMP_FILE_SUFFIX);
				tableFile.upload(table.getHashMapObjectStream());
				
				// Prepare the parser file
				parserFile = new MatlabFileTransfer(matlabServer, Matlab.MATLAB_HASHMAP_SCRIPT);
				
				// Prepare the MATLAB code
				code = new MatlabCode(snippet, matlabType, 
						parserFile.getServerPath(), 
						codeFile.getServerPath(),
						tableFile.getServerPath());
				codeFile.upload(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getServerPath(), false, true);
				
				// Run the snippet
				matlabServer.acquireMatlabProxy();
				matlabServer.eval(cmd);
				MatlabCode.checkForScriptErrors(matlabServer);
				matlabServer.eval("disp('exectuted snippet and updated " + Matlab.OUTPUT_VARIABLE_NAME + "')");
				matlabServer.releaseMatlabProxy();
				
				// Get back the data.
				tableFile.download();
				table = new MatlabTable(tableFile.getClientFile());
				table.readHashMapFromTempFolder(exec);
				table.linkedHashMap2KnimeTable(exec);
				
				return table.getBufferedDataTable();
				
			} else if (transferMethod.equals("workspace")) {
				// Prepare the MATLAB snippet code
				code = new MatlabCode(snippet, matlabType, codeFile.getServerPath());
				codeFile.upload(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getServerPath(), true, true);

				// Get a proxy (block it) push the data execute the snippet and pull back the modified data
				matlabServer.acquireMatlabProxy();
				table.pushTable2MatlabWorkspace(matlabServer, matlabType);
				matlabServer.eval(cmd);
				BufferedDataTable outputTable = table.pullTableFromMatlabWorkspace(exec, matlabServer, matlabType);
				matlabServer.releaseMatlabProxy();

				return outputTable;
			} else {
				throw new RuntimeException("Unknown data transfer method '" + transferMethod + 
						"'. Something's worng with your MATLAB scripting preferences");
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public File plotTask(BufferedDataTable inputTable, String transferMethod, String snippet, Integer plotWidth, Integer plotHeight, String matlabType) 
				throws IOException, Exception {
			
			// Get the data ready for conversion
			table = new MatlabTable(inputTable);
			// Prepare a snippet file
			codeFile = new MatlabFileTransfer(matlabServer, Matlab.SNIPPET_TEMP_FILE_PREFIX, Matlab.SNIPPET_TEMP_FILE_SUFFIX);
			// Prepare the plot image file
			plotFile = new MatlabFileTransfer(matlabServer, Matlab.PLOT_TEMP_FILE_PREFIX, Matlab.PLOT_TEMP_FILE_SUFFIX);
			
			if (transferMethod.equals("file")) {
				// Transfer the KNIME table as hash map object dump to the JVM temp-folder
				table.writeHashMapToTempFolder();
				tableFile = new MatlabFileTransfer(matlabServer, table.getTempFile());
				
				// Prepare the data table parser file
				parserFile = new MatlabFileTransfer(matlabServer, Matlab.MATLAB_HASHMAP_SCRIPT);
				
				// Copy the MATLAB script to the temp-directory and get the file name with the random string in it
				codeFile = new MatlabFileTransfer(matlabServer, Matlab.SNIPPET_TEMP_FILE_PREFIX, Matlab.SNIPPET_TEMP_FILE_SUFFIX);
				code = new MatlabCode(snippet, matlabType, 
						parserFile.getServerPath(), 
						codeFile.getServerPath(),
						tableFile.getServerPath(), 
						plotFile.getServerPath(), 
						plotWidth, plotHeight);
				codeFile.upload(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getServerPath(), false, false);
				
				// Execute 
				matlabServer.acquireMatlabProxy();
				matlabServer.eval(cmd);
				MatlabCode.checkForScriptErrors(matlabServer);
				matlabServer.eval("disp('created plot.')");
			    matlabServer.releaseMatlabProxy();

			    // Return the png-image
			    plotFile.download();
				return plotFile.getClientFile();

			} else if (transferMethod.equals("workspace")) {
				// Get a proxy (block it)
				matlabServer.acquireMatlabProxy();

				// Push the table to the input variable in the MATLAB workspace.
				table.pushTable2MatlabWorkspace(matlabServer, matlabType);

				// Add the code to produce a png-file from the plot
				MatlabCode code = new MatlabCode(snippet, matlabType,
						codeFile.getServerPath(), 
						plotFile.getServerPath(), 
						plotWidth, plotHeight);
				codeFile.upload(new ByteArrayInputStream(code.getScript().getBytes()));
				String cmd = code.getScriptExecutionCommand(codeFile.getServerPath(), true, false);

				// Run the snippet it in MATLAB
				matlabServer.acquireMatlabProxy();
				matlabServer.eval(cmd);
				MatlabCode.checkForScriptErrors(matlabServer);
				matlabServer.eval("disp('created plot and updated " + Matlab.INPUT_VARIABLE_NAME + ", " + Matlab.COLUMNS_VARIABLE_NAME + " ')");
				matlabServer.releaseMatlabProxy();

				// Fetch the output from the server
				plotFile.download();

				return plotFile.getClientFile();
			} else {
				throw new RuntimeException("Unknown data transfer method '" + transferMethod + 
						"'. Something's worng with your MATLAB scripting preferences");
			}
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void rollback() throws MatlabConnectionException {
			releaseMatlabProxy();
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void cleanup() {
			if (tableFile != null)
				tableFile.delete();
			if (parserFile != null)
				parserFile.delete();
			if (codeFile != null)
				codeFile.delete();
			if (plotFile != null)
				plotFile.delete();
			if (table != null)
				table.cleanup();
		}
		
		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void acquireMatlabProxy() throws MatlabConnectionException, MatlabInvocationException {
			matlabServer.acquireMatlabProxy();
			matlabServer.eval(MatlabCode.getClearWorkspaceCommand());
			matlabServer.printServerMessage("Execute job: " + this.hostName + ", client " + this.clientNumber);
		}
		
		/**
		 * {@inheritDoc} 
		 */
		@Override
		public void releaseMatlabProxy() throws MatlabConnectionException {
			matlabServer.releaseMatlabProxy();
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

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void printServerMessage(String msg) {
			matlabServer.printServerMessage(msg);
		}		
		
		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void eval(String arg0) throws MatlabInvocationException {
			matlabServer.eval(arg0);
		}
		
		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void feval(String arg0, Object... arg1)
				throws MatlabInvocationException {
			matlabServer.feval(arg0, arg1);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public Object getVariable(String arg0) throws MatlabInvocationException {
			return matlabServer.getVariable(arg0);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public Object[] returningEval(String arg0, int arg1)
				throws MatlabInvocationException {
			return matlabServer.returningEval(arg0, arg1);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public Object[] returningFeval(String arg0, int arg1, Object... arg2)
				throws MatlabInvocationException {
			return matlabServer.returningFeval(arg0, arg1, arg2);
		}

		/**
		 * {@inheritDoc}  
		 */
		@Override
		public void setVariable(String arg0, Object arg1)
				throws MatlabInvocationException {
			matlabServer.setVariable(arg0, arg1);
		}
		
	}

	
}


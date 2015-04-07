package de.mpicbg.knime.scripting.matlab.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;


/**
 * This class can be considered to be a local MATLAB server for MATLAB scripting-integration
 * plugin for KNIME. It manages a single running MATLAB application that can be controlled
 * by the KNIME nodes.
 * 
 * TODO: add connection timeout
 * TODO: Connection problems are not handled well yet.
 * 		 1) if there is not network and MATLAB can't check out a license it hangs and if interrupted, thinks that matlab already runs.
 * 
 * @author Felix Meyenhofer
 */
public class MatlabController {
	
	/** keep one single class instance */
	private static MatlabController instance = new MatlabController();
	
	/** Execution mode (as server or on local machine) */
	private static boolean isServer = false;

//	/** Thread number (for identification during debugging) */
//	private Integer threadNumber = 1;
	
	/** Total count of threads connecting to MATLAB */
	private static Integer threadCount = 0;
	
	/** Factory to control the MATLAB session */
	static MatlabProxyFactory proxyFactory;
	
	/** MATLAB access queue */
	static ArrayBlockingQueue<MatlabProxy> proxyQueue;// = new ArrayBlockingQueue<MatlabProxy>(1);
	
	/** Number of allowed proxies (set during instantiation of the class) */
	static int proxyQueueSize = 1;
	
	
	/**
	 * Constructor
	 */
	private MatlabController() {
		// Prevent multiple instantiation
	}
	
	/**
	 * Get the instance of the Matlab controller.
	 * 
	 * @return Matlab controller (single) instance.
	 * @throws MatlabConnectionException
	 */
	public static synchronized MatlabController getInstance() throws MatlabConnectionException {
		if (MatlabController.instance == null)
			MatlabController.instance = new MatlabController();
		
		// Determine the total number of threads and the number of this thread
		threadCount++;
					
//		this.threadNumber = threadCount;
		
		// Create the proxy factory (exactly once).
		if (proxyFactory == null) {
			MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().
					setUsePreviouslyControlledSession(true).
					build();
			proxyFactory = new MatlabProxyFactory(options);
		} 
		
		// If the controller is owned by the MatlabServer we start MATLAB immediately.
		if (isServer)
			connect();
		
		return MatlabController.instance;
	}
	
	/**
	 * Set the Matlab controller parameter
	 * 
	 * @param sessions
	 * @param executionMode
	 */
	public synchronized void setParameter(int sessions, boolean executionMode) {
		System.out.println("Overwrite MatlabController paramter: " + proxyQueueSize + "->" + sessions + ", " + isServer + "->" + executionMode +".");
		
		isServer = executionMode;
		proxyQueueSize = sessions;
	}
	
	/**
	 * Connect to the MATLAB application. If the object was initialized properly
	 * and the MATLAB application it started is still running, there is nothing to do.
	 * Otherwise this method starts a new MATLAB application. 
	 * 
	 * @throws MatlabConnectionException
	 */	
	public static synchronized void connect() throws MatlabConnectionException {
		if (proxyQueue == null) {
			System.out.println("MATLAB: starting applicaton...");
			proxyQueue = new ArrayBlockingQueue<MatlabProxy>(proxyQueueSize);
			// Use the factory to get a running MATLAB session
			for (int i = 0; i < proxyQueueSize; i++) {
				proxyFactory.requestProxy(new MatlabProxyFactory.RequestCallback() {
		            @Override
		            public void proxyCreated(MatlabProxy proxy) {
		            	// Once the session is running, add the proxy object to the proxy queue
		                proxyQueue.add(proxy);
		                // Add a disconnection listener, so this controller can react if the MATLAB 
		                // application is closed (for instance by the user)
		                proxy.addDisconnectionListener(new MatlabProxy.DisconnectionListener() {				
							@Override
							public void proxyDisconnected(MatlabProxy proxy) {
								System.out.println("MATLAB application disconnected.");
								proxyQueue = null;
								// Try to restart MATLAB immediately. The connect method makes sure it only happens once (provided that the first attempt is successful).
								if (isServer) {
									try {
										connect();
									} catch (MatlabConnectionException e) {
										System.err.println("Unable to restart MATLAB application(s). You need to restart the server!");
										e.printStackTrace();
									}
								}
							}
						});
		            }
		        });
			}
			
			// Clean the command window and state what happened
			List<MatlabProxy> tempProxyHolder = new ArrayList<MatlabProxy>(proxyQueueSize);
			
			for (int i = 0; i < proxyQueueSize; i++) {
	            try {
	            	tempProxyHolder.add(getQueue().take());
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
			}
			for (int i = 0; i < proxyQueueSize; i++) {
				try {
					MatlabProxy proxy = tempProxyHolder.remove(0); 
					proxy.eval("clc");
					proxy.eval("disp('Started from KNIME (MATLAB scripting integration)')");
					getQueue().put(proxy);
					
				} catch (MatlabInvocationException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
            System.out.println("...running.");
		} else {
//			System.out.println("MATLAB application is running.");
		}
	}
	
	
	/**
	 * Getter for the MATLAB connector queue
	 * 
	 * @return
	 */
	public static synchronized ArrayBlockingQueue<MatlabProxy> getQueue() {
		return proxyQueue;
	}
	
	public static synchronized int getReferenceCount() {
		return threadCount;
	}
	
	
	/**
	 * Get a proxy from the queue.
	 * This should be succeeded by a call to {@link #returnProxyToQueue}
	 * 
	 * @return Proxy object
	 * @throws MatlabConnectionException 
	 */
	public MatlabProxy acquireProxyFromQueue() throws MatlabConnectionException {
		// Establish a session connection
		connect();
		// Get access to the session
		try {
			return proxyQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("This is bad. The queue is probably corrupted. You need to close and reopen the Workflow.");
		}
	}
	
	
	/**
	 * Put the proxy back into the queue.
	 * This should be preceded by a call to {@link #acquireProxyFromQueue()}
	 * 
	 * @param proxy
	 */
	public void returnProxyToQueue(MatlabProxy proxy) {
		try {
			proxyQueue.put(proxy);
		} catch (InterruptedException e) {
			System.err.println("This is bad. The queue is probably corrupted. You need to reopen the Workflow.");
			e.printStackTrace();
		}
	}
	
}

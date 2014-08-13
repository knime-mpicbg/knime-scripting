package de.mpicbg.knime.scripting.matlab.srv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.PermissiveSecurityManager;


/**
 * This class can be considered to be a local MATLAB server for MATLAB scripting-integration
 * plugin for KNIME. It manages a single running MATLAB application that can be controlled
 * by the KNIME nodes.
 * 
 * TODO: add connection timeout
 * TODO: The proxy queue has currently a length of 1. I should be fairly easy to pass a parameter
 * from node dialog that controls the number of MATLAB instances. This way the task could be distributed
 * on several MATLAB applications
 * 
 * @author Felix Meyenhofer
 */
public class MatlabController {

	/** Thread number (for identification during debugging) */
	private Integer threadNumber;
	
	/** Total count of threads connecting to MATLAB */
	static Integer threadCount;
	
	/** Factory to control the MATLAB session */
	static MatlabProxyFactory proxyFactory;
	
	/** MATLAB access queue */
	static ArrayBlockingQueue<MatlabProxy> proxyQueue;// = new ArrayBlockingQueue<MatlabProxy>(1);
	
	/** Number of allowed proxies (set during instantiation of the class) */
	static int proxyQueueSize; 

	
	/**
	 * Constructor of a local MATLAB connector.
	 * This starts a single MATLAB session that will be controlled with the JMI wrapper
	 * matlabcontrol.
	 * All the instances of this class will communicate with one and the same MATLAB 
	 * session. Hence this class handles it's own queue.
	 * 
	 * @param name of the thread
	 * @throws MatlabConnectionException
	 */
	public MatlabController(int sessions) throws MatlabConnectionException {
		// Set a very permissive security manager (beware this could be an entry point for abuse)
		System.setSecurityManager(new PermissiveSecurityManager());
		
		synchronized(this) {
			proxyQueueSize = sessions;
			
			// Determine the total number of threads and the number of this thread
			if (threadCount == null) {
				threadCount = 1;
			} else {
				threadCount++;
			}
			this.threadNumber = threadCount;
			
			// Create the proxy factory (exactly once).
			if (proxyFactory == null) {
				MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().
						setUsePreviouslyControlledSession(true).
						build();
				proxyFactory = new MatlabProxyFactory(options);
			} 
			
			connect();
		}
		
		System.out.println("MATLAB: created thread " +  threadNumber );
	}
	
	
	/**
	 * Connect to the MATLAB application. If the object was initialized properly
	 * and the MATLAB application it started is still running, there is nothing to do.
	 * Otherwise this method starts a new MATLAB application. 
	 * 
	 * @throws MatlabConnectionException
	 */	
	public synchronized void connect() throws MatlabConnectionException {
		System.out.println("MATLAB: starting...");
		if (proxyQueue == null) {
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
								proxyQueue = null;
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
			System.out.println("...already running.");
		}
	}
	
	
	/**
	 * Getter for the MATLAB connector queue
	 * 
	 * @return
	 */
	public synchronized ArrayBlockingQueue<MatlabProxy> getQueue() {
		return proxyQueue;
	}
	
	
	/**
	 * Get a proxy from the queue.
	 * This should be succeeded by a call to {@link #returnProxyToQueue}
	 * 
	 * @return Proxy object
	 * @throws MatlabConnectionException 
	 */
	public synchronized MatlabProxy acquireProxyFromQueue() throws MatlabConnectionException {
		// Establish a session connection
		connect();
		// Get access to the session
		try {
			return proxyQueue.take();
		} catch (InterruptedException e) {
			System.err.println("This is bad. The queue is probably corrupted. You need to reopen the Workflow.");
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Put the proxy back into the queue.
	 * This should be preceded by a call to {@link #acquireProxyFromQueue()}
	 * 
	 * @param proxy
	 */
	public synchronized void returnProxyToQueue(MatlabProxy proxy) {
		try {
			proxyQueue.put(proxy);
		} catch (InterruptedException e) {
			System.err.println("This is bad. The queue is probably corrupted. You need to reopen the Workflow.");
			e.printStackTrace();
		}
	}
	

	/**
	 * Getter for the thread number
	 * 
	 * @return
	 */
	public Integer getThreadNumber() {
		return this.threadNumber;
	}
	
}

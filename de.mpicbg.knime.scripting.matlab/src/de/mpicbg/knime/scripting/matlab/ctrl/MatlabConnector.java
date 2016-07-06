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
 * TODO: Connection problems are not handled well yet:
 * 		 if there is not network and MATLAB can't check out a license it hangs and if interrupted, thinks that matlab already runs.
 * 
 * @author Felix Meyenhofer
 */
public class MatlabConnector {
	
	/** keep one single class instance */
	private static MatlabConnector instance;
	
	/** Total count of threads connecting to MATLAB */
	private static Integer threadCount = 0;
	
	/** Factory to control the MATLAB session */
	static MatlabProxyFactory proxyFactory;
	
	/** MATLAB access queue */
	static ArrayBlockingQueue<MatlabProxy> proxyQueue = new ArrayBlockingQueue<MatlabProxy>(10);
	
	/** Number of allowed proxies (set during instantiation of the class) */
	static int proxyQueueSize = 1;
	
	/** Difference of the currently available MATLAB sessions and the set quota */
	private static int proxyQueueDifference = 0;
	
	/**
	 * Constructor
	 */
	private MatlabConnector() {
		// Prevent multiple instantiation
	}
	
	/**
	 * Get the instance of the MatlabConnector
	 * 
	 * @param numberOfMatlabSessions allowed
	 * @return the Matlab connector singleton
	 * @throws MatlabConnectionException
	 * @throws MatlabInvocationException
	 * @throws InterruptedException
	 */
	public static synchronized MatlabConnector getInstance(int numberOfMatlabSessions) throws MatlabConnectionException, 
																					  MatlabInvocationException, 
																					  InterruptedException {
		// Initialize ONCE if necessary and set the queue size and difference.
		if (MatlabConnector.instance == null) {
			MatlabConnector.instance = new MatlabConnector();
			proxyQueueSize = numberOfMatlabSessions;
			proxyQueueDifference = proxyQueue.size() - proxyQueueSize;
		}
		
		// Determine the total number of threads and the number of this thread
		threadCount++;
		
		// Create the proxy factory (exactly once).
		if (proxyFactory == null) {
			MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().
					setUsePreviouslyControlledSession(true).
					build();
			proxyFactory = new MatlabProxyFactory(options);
		}
		
		return MatlabConnector.instance;
	}
	
	/**
	 * Setter for the number of Matlab instances
	 * @param newSize
	 */
	public static synchronized void setProxyQueueSize(int newSize) {
		if (newSize != proxyQueueSize) {
			proxyQueueDifference = proxyQueueSize - newSize;
			proxyQueueSize = newSize;			
		}
	}
	
	/**
	 * Update the Matlab instance queue
	 * 
	 * @throws MatlabConnectionException
	 * @throws MatlabInvocationException
	 * @throws InterruptedException
	 */
	private static synchronized void updateProxyQueue() throws MatlabConnectionException, 
															   MatlabInvocationException, 
															   InterruptedException {
		for (int i = 0; i < Math.abs(proxyQueueDifference); i++) {
			if (proxyQueueDifference < 0) {
				requestMatlabProxy();
			} else {
				MatlabProxy proxy = proxyQueue.take();
				proxy.disconnect();
				proxyQueue.remove(proxy);					
			}
			
		}
		
		proxyQueueDifference = 0;
	}
	
	/**
	 * Try to get a proxy for a Matlab instance
	 * @throws MatlabConnectionException
	 * @throws MatlabInvocationException
	 * @throws InterruptedException
	 */
	private static synchronized void requestMatlabProxy() throws MatlabConnectionException, 
																 MatlabInvocationException, 
																 InterruptedException {
		System.out.println("MATLAB: starting new session...");
		
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
						proxyQueue.remove(proxy);
						proxyQueueDifference--;
						System.out.println("MATLAB application disconnected. Remaining running sessions: " + proxyQueue.size() + ", session difference: " + proxyQueueDifference);
					}					
				});
            }
		});
		
		cleanupMatlabConsole();
	}
	
	/**
	 * Utility to clean the output in the Matlab console
	 */
	private static synchronized void cleanupMatlabConsole() {
		// Move the proxies from the blocking queue to a temporary list
		List<MatlabProxy> tempProxyHolder = new ArrayList<MatlabProxy>(proxyQueueSize);
		for (int i = 0; i < proxyQueueSize; i++) {
            try {
            	tempProxyHolder.add(proxyQueue.take());
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
		}
		
		// Execute the MATLAB commands and move the proxies back to the array blocking queue.
		for (int i = 0; i < proxyQueueSize; i++) {
			try {
				MatlabProxy proxy = tempProxyHolder.remove(0); 
				proxy.eval("clc;");
				proxy.eval("disp('Started from KNIME (MATLAB scripting integration)');");
				proxyQueue.put(proxy);
			
			} catch (MatlabInvocationException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the number of threads
	 * 
	 * @return number of threads
	 */
	public static synchronized int getReferenceCount() {
		return threadCount;
	}
	
	/**
	 * Get a proxy from the queue.
	 * This should be succeeded by a call to {@link #returnProxyToQueue}
	 * 
	 * @return Proxy object
	 * @throws MatlabConnectionException 
	 * @throws InterruptedException 
	 * @throws MatlabInvocationException 
	 */
	public MatlabProxy acquireProxyFromQueue() throws MatlabConnectionException, 
													  MatlabInvocationException, 
													  InterruptedException {
		// Update the queue (in case a MATLAB session is closed, we want to restart a new one
		// if it is requested by the node, and not before!)
		updateProxyQueue();

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

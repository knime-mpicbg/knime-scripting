package de.mpicbg.knime.scripting.matlab.srv;

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
	static ArrayBlockingQueue<MatlabProxy> proxyQueue = new ArrayBlockingQueue<MatlabProxy>(1);

	
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
	public MatlabController() throws MatlabConnectionException {
		// Set a very permissive security manager (beware this could be an entry point for abuse)
		System.setSecurityManager(new PermissiveSecurityManager());
		
		synchronized(this) {
			
			// Determine the total number of threads and the number of this thread
			if (threadCount == null) {
				threadCount = 1;
			} else {
				threadCount++;
			}
			threadNumber = threadCount;
			
			// Create the proxy factory (exactly once).
			if (proxyFactory == null) {
				MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().
						setUsePreviouslyControlledSession(true).
						build();
				proxyFactory = new MatlabProxyFactory(options);
			} 
		}
		
		// Establish a session connection
		connect();
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
		if (proxyQueue.size() == 0) {
			// Use the factory to get a running MATLAB session
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
						}
					});
	            }
	        });
			
			// Clean the command window and state what happened
            try {
            	MatlabProxy proxy = getQueue().take();
				proxy.eval("clc");
				proxy.eval("disp('Started from KNIME (MATLAB scripting integration)')");
				getQueue().put(proxy);
			} catch (MatlabInvocationException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e2) {
				e2.printStackTrace();
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
	 * Getter for the thread number
	 * 
	 * @return
	 */
	public Integer getThreadNumber() {
		return this.threadNumber;
	}
	
	
	/**
	 * Execute a string in the command line of the running MATLAB application.
	 * 
	 * @param cmd String to execute on the MATLAB command line
	 */
	public void evaluate(String cmd) {
		try {
			System.out.println("MATLAB thread " +  threadNumber + ": acquiring control...");
			// Make sure we have a connection
			connect();
			// Acquire proxy from the queue
			ArrayBlockingQueue<MatlabProxy> queue = getQueue();
			MatlabProxy proxy = queue.take();
			// Executing MATLAB command
			System.out.println("MATLAB thread " +  threadNumber + ": executing...");
			proxy.eval(cmd);
			// Returning the proxy to the queue
			System.out.println("MATLAB thread " +  threadNumber + ": finished!");
			queue.put(proxy);
		} catch (InterruptedException e) {
			System.err.println("MATLAB: thread " +  threadNumber + ": interrupted!");
			e.printStackTrace();
		} catch (MatlabInvocationException e2) {
			System.err.println("MATLAB: in thread " +  threadNumber + " the execution string contains syntax error(s)!");
			e2.printStackTrace();
		} catch (MatlabConnectionException e3) {
			System.err.println("MATLAB: thread " +  threadNumber + "  was unable to connect to matlab");
			e3.printStackTrace();
		}
	}
	
}

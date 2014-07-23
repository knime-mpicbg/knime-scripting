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
 * @author Felix Meyenhofer
 */
public class MatlabLocal {

	/** Session number (for identification during debugging) */
	private Integer sessionNumber;
	/** Total count of MATLAB sessions */
	static Integer sessionCount;
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
	public MatlabLocal() throws MatlabConnectionException {
		// Set a very permissive security manager (beware this could be an entry point for abuse)
		System.setSecurityManager(new PermissiveSecurityManager());
		
		synchronized(this) {
			
			// Determine the total number of threads and the number of this thread
			if (sessionCount == null) {
				sessionCount = 1;
			} else {
				sessionCount++;
			}
			sessionNumber = sessionCount;
			
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
		System.out.println("MATLAB: created thread " +  sessionNumber );
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
	 * Getter for the MATLAB connector queue.
	 * 
	 * @return
	 */
	public synchronized ArrayBlockingQueue<MatlabProxy> getQueue() {
		return proxyQueue;
	}
	
	
	/**
	 * Execute a string in the command line of the running MATLAB application.
	 * 
	 * @param cmd String to execute on the MATLAB command line
	 */
	public void execute(String cmd) {
		try {
			System.out.println("MATLAB thread " +  sessionNumber + ": acquiring control...");
			// Make sure we have a connection
			connect();
			// Acquire proxy from the queue
			ArrayBlockingQueue<MatlabProxy> queue = getQueue();
			MatlabProxy proxy = queue.take();
			// Executing MATLAB command
			System.out.println("MATLAB thread " +  sessionNumber + ": executing...");
			proxy.eval("disp('session " + sessionNumber + ":')");
			proxy.eval(cmd);
			// Returning the proxy to the queue
			System.out.println("MATLAB thread " +  sessionNumber + ": finished!");
			queue.put(proxy);
		} catch (InterruptedException e) {
			System.err.println("MATLAB: thread " +  sessionNumber + ": interrupted!");
			e.printStackTrace();
		} catch (MatlabInvocationException e2) {
			System.err.println("MATLAB: in thread " +  sessionNumber + " the execution string contains syntax error(s)!");
			e2.printStackTrace();
		} catch (MatlabConnectionException e3) {
			System.err.println("MATLAB: thread " +  sessionNumber + "  was unable to connect to matlab");
			e3.printStackTrace();
		}
	}
	
}

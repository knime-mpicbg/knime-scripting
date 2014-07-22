package de.mpicbg.knime.scripting.matlab.srv;

import java.util.concurrent.ArrayBlockingQueue;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;


class RunnableBee extends Thread {
	
	private Thread t;
	private String threadName;
	
	static MatlabProxyFactory factory;
	static ArrayBlockingQueue<MatlabProxy> proxyQueue;

	
	RunnableBee( String name) throws MatlabConnectionException {
		threadName = name;
		System.out.println("Creating " +  threadName );
	}
	
	
	public synchronized void initialize() throws MatlabConnectionException {
//		synchronized(this) {
			if (factory == null) {
				System.out.println("Starting MATLAB.");
				proxyQueue = new ArrayBlockingQueue<MatlabProxy>(1);		
			
				MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();
				factory = new MatlabProxyFactory(options);
				factory.requestProxy(new MatlabProxyFactory.RequestCallback() {
		            @Override
		            public void proxyCreated(MatlabProxy proxy)
		            {
		                proxyQueue.add(proxy);
		            }
		        });
			} else {
				System.out.println("Been there done that.");
			}
//		}
	}
	
//	public synchronized MatlabProxyFactory getFactory() {
//		return this.factory;
//	}
//	
	public synchronized ArrayBlockingQueue<MatlabProxy> getQueue() {
		return proxyQueue;
	}
	
	@Override
	public void run() {
		System.out.println("Thread " +  threadName + " running");
		try {
			ArrayBlockingQueue<MatlabProxy> queue = getQueue();
			MatlabProxy proxy = queue.take();
			proxy.eval("disp('" + threadName + "')");
			proxy.eval("a = rand(10000);");
			Object res = proxy.getVariable("a");
			System.out.println(res);
			queue.put(proxy);
//			proxy.disconnect();
		} catch (InterruptedException e) {
			System.out.println("Thread " +  threadName + " interrupted.");
			e.printStackTrace();
		} catch (MatlabInvocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Thread " +  threadName + " exiting.");
	}

	public void start () {
		System.out.println("Thread " +  threadName + " starting");
		if (t == null)
		{
			t = new Thread (this, threadName);
			t.start ();
		}
	}
	
}



public class MatCtrlThreads {
	
	public static void main(String[] args) throws MatlabConnectionException, InterruptedException, MatlabInvocationException {
		RunnableBee bee1 = new RunnableBee("Bee-1");
		RunnableBee bee2 = new RunnableBee("Bee-2");
		
		bee1.initialize();
		bee1.start();
		
		
		bee2.initialize();
		bee2.start();
		
		bee1.join();
		bee2.join();
	}	

}
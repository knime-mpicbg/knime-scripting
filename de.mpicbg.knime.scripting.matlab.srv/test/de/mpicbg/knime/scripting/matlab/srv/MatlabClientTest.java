package de.mpicbg.knime.scripting.matlab.srv;

import gnu.cajo.Cajo;

import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.RemoteException; // caused by network related errors
import java.util.concurrent.ArrayBlockingQueue;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;


	

//client method sets need not be public
//declaring RemoteException is optional, but a nice reminder
interface SuperSet {  
	void baz() throws RemoteException;
} 

//the order of the client method set does not matter
interface ClientSet extends SuperSet {
	boolean bar(Integer quantum) throws RemoteException;
	Object foo(String barbaz, int foobar) throws RemoteException;
} 



class MWorker extends Thread {
	
	private Thread t;
	private String threadName;
	
	private MClient client;

	
	MWorker(String name) throws Exception {
		threadName = name;
		System.out.println("Creating " +  threadName );
		client = new MClient();
		System.out.println("\tinstanciated client.");
	}
	
	
	
	@Override
	public void run() {
		System.out.println("Thread " +  threadName + " running");
		try {
			Thread.sleep(1000);
			try {
				client.actout();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
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


class MClient {
	
	ClientSet actions;
	
	MClient() throws Exception {
		Cajo cajo = new Cajo(0, null, null);
		cajo.register("Client", 1198);
		Thread.currentThread().sleep(100); 
		
		Object refs[] = cajo.lookup(ClientSet.class);
		if (refs.length > 0) { // compatible server objects found
			System.out.println("Found " + refs.length);
			ClientSet actions = (ClientSet)cajo.proxy(refs[0], ClientSet.class);
		} else {
			System.out.println("No server objects found");
		}
	}
	
	public void actout() throws RemoteException {
		actions.baz();
		System.out.println(actions.bar(new Integer(77)));
		System.out.println(actions.foo(null, 99));
	}
	
}


public class MatlabClientTest {
	public static void main(String args[]) throws Exception { // unit test
		MWorker bee1 = new MWorker("Worker-1");
		MWorker bee2 = new MWorker("Worker-2");
		
		bee1.start();
		bee2.start();
		
		bee1.join();
		bee2.join();
		
		System.exit(0); // nothing else left to do, so we can shut down
	}
}

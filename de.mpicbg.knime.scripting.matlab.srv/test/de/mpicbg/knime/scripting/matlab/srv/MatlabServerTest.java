package de.mpicbg.knime.scripting.matlab.srv;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import gnu.cajo.Cajo; // The cajo implementation of the Grail

public class MatlabServerTest {

	public static class Test { 
		// remotely callable classes must be public
		// though not necessarily declared in the same class
		private final String greeting;
		
		private static  ArrayBlockingQueue<String> proxyQueue;
		
		private List<String> proxyHolder = new ArrayList<String>(1);


		// no silly requirement to have no-arg constructors
		public Test(int numberOfProxies) {
			greeting = "hello";
			
			proxyQueue = new ArrayBlockingQueue<String>(numberOfProxies);
			for (int i = 1; i <= numberOfProxies; i++) {
				try {
					proxyQueue.put("dummy " + i);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// all public methods, instance or static, will be remotely callable
		public String foo(Object bar, int count) {
			System.out.println("foo called w/ " + bar + ' ' + count + " count");
			return greeting;
		}
		public Boolean bar(int count) {
			System.out.println("bar called w/ " + count + " count");
			return Boolean.TRUE;
		}
		public boolean baz() {
			System.out.println("baz called");
			return true;
		}
		public String other() { // functionality not needed by the test client
			return "This is extra stuff";
		}
		public void acquireProxy() throws InterruptedException {
			proxyHolder.set(0,getProxy());
		}
		public void returnProxy() throws InterruptedException {
			setProxy(proxyHolder.remove(0));
		}
		
		
		private synchronized String getProxy() throws InterruptedException {
			return proxyQueue.take();
		}
		private synchronized void setProxy(String proxy) throws InterruptedException {
			proxyQueue.put(proxy);
		}
		
	} // arguments and return objects can be custom or common to server and client

	public static void main(String args[]) throws Exception { // unit test
		Cajo cajo = new Cajo(0, null, null);
		System.out.println("Server running");
		cajo.export(new Test(1));
	}
}

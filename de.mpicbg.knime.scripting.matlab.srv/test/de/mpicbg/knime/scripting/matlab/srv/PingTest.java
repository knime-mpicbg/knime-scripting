package de.mpicbg.knime.scripting.matlab.srv;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class PingTest {
	
	public static void main(String[] args) {
		
		Socket socket = new Socket();
		boolean reachable = false;
		try {
//		    socket = new Socket("medpc359.unifr.ch", 1198);
			InetSocketAddress inet = new InetSocketAddress("medpc359.unifr.ch", 1198);
			
			InetAddress host = InetAddress.getByName("medpc3590.unifr.ch");
		    socket.setSoTimeout(5000);
		    socket.connect(inet);
		    reachable = true;
		    System.out.println("online");
		    
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {            
		    if (socket != null) {
		    	try { 
		    		socket.close(); 
		    	} catch(IOException e) {
		    		System.out.println("Could not close socket.");
		    	}
		    }
		}
		
		System.out.println("" + reachable);
		
	}

}

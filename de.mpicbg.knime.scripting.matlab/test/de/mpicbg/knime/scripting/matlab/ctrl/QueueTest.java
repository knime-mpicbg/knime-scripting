package de.mpicbg.knime.scripting.matlab.ctrl;

import java.util.concurrent.ArrayBlockingQueue;

public class QueueTest {
	
	public static void main(String[] args) throws InterruptedException {
		
		ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(2);
		
		queue.put(1);
		
		System.out.println("Queue size: " + queue.size());
		System.out.println("Queue is empty: " + queue.isEmpty());
		
	}

}

package org.unbiquitous.network.http;

import java.util.ArrayDeque;
import java.util.Queue;

import org.unbiquitous.network.http.connection.WebSocketDevice;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManager;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.radar.Radar;
import org.unbiquitous.uos.core.network.radar.RadarListener;

public class WebSocketRadar implements Radar {

	private RadarListener listener;
	private Queue<WebSocketDevice> enteredQueue = new ArrayDeque<WebSocketDevice>();
	private Queue<WebSocketDevice> leftQueue = new ArrayDeque<WebSocketDevice>();

	private boolean running = true;
	
	//Cria um objeto que será o "monitor" desta Thread. 
	//Ele possui um tipo próprio para facilitar debug se houver erro
	private static final class Lock { }
	private final Object lock = new Lock();

	public WebSocketRadar(RadarListener listener) {
		this.listener = listener;
	}
	
	public void run() {
		while (running) {
			
			while (!enteredQueue.isEmpty()) {
				listener.deviceEntered(enteredQueue.poll());
			}
			
			while (!leftQueue.isEmpty()) {
				listener.deviceLeft(leftQueue.poll());
			}
			
			synchronized (lock) 
			{
				try {
					lock.wait();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void startRadar() {
		running = true;
	}

	public void stopRadar() {
		running = false;
	}

	public void setConnectionManager(ConnectionManager connectionManager) {
		WebSocketConnectionManager mng = (WebSocketConnectionManager) connectionManager;
		mng.setRadar(this);
	}

	public void deviceEntered(WebSocketDevice device) {
		enteredQueue.add(device);
		synchronized (lock) {
			lock.notifyAll(); //Acorda a thread
		}
	}

	public void deviceLeft(WebSocketDevice device) {
		leftQueue.add(device);
		synchronized (lock) {
			lock.notifyAll(); //Acorda a thread
		}
	}

}

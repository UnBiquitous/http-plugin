package org.unbiquitous.network.http;

import java.util.ArrayDeque;
import java.util.Queue;

import org.unbiquitous.network.http.connection.WebSocketDevice;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManager;
import org.unbiquitous.uos.core.network.radar.Radar;
import org.unbiquitous.uos.core.network.radar.RadarListener;

public class WebSocketRadar implements Radar {

	private RadarListener listener;
	private Queue<WebSocketDevice> enteredQueue = new ArrayDeque<WebSocketDevice>();
	private Queue<WebSocketDevice> leftQueue = new ArrayDeque<WebSocketDevice>();

	private boolean running = true;

	public WebSocketRadar(RadarListener listener) {
		this.listener = listener;
	}

	public void run() {
		while(running){
			while(!enteredQueue.isEmpty()){
				listener.deviceEntered(enteredQueue.poll());
			}
			while(!leftQueue.isEmpty()){
				listener.deviceLeft(leftQueue.poll());
			}
			Thread.yield();
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
	}

	public void deviceLeft(WebSocketDevice device) {
		leftQueue.add(device);
	}

}

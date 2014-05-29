package org.unbiquitous.network.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.exceptions.NetworkException;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public class WebSocketChannelManager implements ChannelManager {
	private WebSocketDevice device = new WebSocketDevice();
	private Map<String, WebSocketConnection> connections = new HashMap<>();
	private Map<String, String> sessionToUUID = new HashMap<>();
	private ConnectionManagerListener listener;

	public WebSocketChannelManager(ConnectionManagerListener listener) {
		this.listener = listener;
	}
	
	public ClientConnection openPassiveConnection(String networkDeviceName)
			throws NetworkException, IOException {
		throw new RuntimeException("Passive connections are not supported by websockets");
	}

	public NetworkDevice getAvailableNetworkDevice() {
		return device;
	}

	public void tearDown() throws NetworkException, IOException {}
	
	
	public void addConnection(String uuid, String sessionId, WebSocketConnection conn){
		connections.put(uuid, conn);
		sessionToUUID.put(sessionId, uuid);
	}
	
	public boolean knows(String uuid){
		return connections.containsKey(uuid);
	}
	
	public WebSocketConnection getConnection(String sessionId){
		String uuid = sessionToUUID.get(sessionId);
		return connections.get(uuid);
	}
	
	@Override
	public ClientConnection openActiveConnection(String uuid) throws NetworkException, IOException {
		return connections.get(uuid);
	}

	public ConnectionManagerListener getListener() {
		return listener;
	}
}

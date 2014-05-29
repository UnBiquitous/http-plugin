package org.unbiquitous.network.http.connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.exceptions.NetworkException;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public class WebSocketChannelManager implements ChannelManager {
	private WebSocketDevice device = new WebSocketDevice();
	private ConnectionManagerListener listener;
	
	private Map<String, WebSocketConnection> connections = new HashMap<>();
	private Map<String, String> sessionToUUID = new HashMap<>();
	private Set<String> busyConnections = new HashSet<>();
	private long nextConnectionId = 1;
	
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
	
	public void notifyListener(WebSocketConnection conn){
		synchronized (busyConnections) {
			String uuid = conn.getClientDevice().getNetworkDeviceName();
			if (!busyConnections.contains(uuid)){
				listener.handleClientConnection(conn);
			}else{
				busyConnections.remove(uuid);
			}
		}
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
		busyConnections.add(uuid);
		return connections.get(uuid);
	}

	
}

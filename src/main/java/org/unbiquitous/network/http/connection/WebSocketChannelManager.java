package org.unbiquitous.network.http.connection;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import javax.websocket.Session;

import org.unbiquitous.network.http.WebSocketRadar;
import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.exceptions.NetworkException;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public class WebSocketChannelManager implements ChannelManager {
	private WebSocketDevice device = new WebSocketDevice();
	private ConnectionManagerListener listener;
	
	private Map<UUID, WebSocketConnection> connections = new HashMap<>();
	private Map<String, Session> sessions = new HashMap<>();
	private Map<String, String> sessionToUUID = new HashMap<>();
	private Set<UUID> myOpenedConnections = new HashSet<>();
	
	private Queue<WebSocketDevice> enteredQueue = new ArrayDeque<>();
	private WebSocketRadar radar;
	
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
	
	
	public void addConnection(String uuid, String sessionId, Session session){
		sessions.put(uuid, session);
		sessionToUUID.put(sessionId, uuid);
	}
	
	public void notifyListener(WebSocketConnection conn){
		UUID connectionID = null;
		connectionID = conn.getConnectionId();
		synchronized (connectionID) {
			if (!myOpenedConnections.contains(conn.getConnectionId())){
				listener.handleClientConnection(conn);
				myOpenedConnections.remove(conn.getConnectionId());
			}
		}
	}
	
	public boolean knows(String uuid){
		return sessions.containsKey(uuid);
	}
	
	public WebSocketConnection getConnection(String sessionId, UUID connectionId){
		String uuid = sessionToUUID.get(sessionId);
		
		WebSocketConnection conn = connections.get(connectionId);
		if (conn == null){
			Session session = sessions.get(uuid);
			WebSocketDevice clientDevice = new WebSocketDevice(uuid);
			conn = new WebSocketConnection(clientDevice, session, connectionId);
		}
		return conn;
	}
	
	@Override
	public ClientConnection openActiveConnection(String uuid) throws NetworkException, IOException {
		Session session = sessions.get(uuid);
		WebSocketDevice clientDevice = new WebSocketDevice(uuid);
		WebSocketConnection conn = new WebSocketConnection(clientDevice, session);
		connections.put(conn.getConnectionId(), conn);
		myOpenedConnections.add(conn.getConnectionId());
		return conn;
	}

	public void deviceEntered(String uuid){
		WebSocketDevice device = new WebSocketDevice(uuid);
		if(radar == null){
			enteredQueue.add(device);
		}else{
			radar.deviceEntered(device);
		}
	}

	public void deviceLeft(Session session){
		String uuid = sessionToUUID.get(session.getId());
		radar.deviceLeft(new WebSocketDevice(uuid));
	}
	
	public void setRadar(WebSocketRadar radar) {
		this.radar = radar;
		while(!enteredQueue.isEmpty()){
			radar.deviceEntered(enteredQueue.poll());
		}
	}
}

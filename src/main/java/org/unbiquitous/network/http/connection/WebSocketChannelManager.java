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
	
	private Map<UUID, WebSocketConnection> connections = new HashMap<UUID, WebSocketConnection>(){
		public WebSocketConnection put(UUID key, WebSocketConnection value) {
			System.out.println(key+" relates to "+value.getClientDevice().getNetworkDeviceName());
			return super.put(key, value);
		};
	};
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
			System.out.println("new "+connectionId+" at "+getAvailableNetworkDevice().getNetworkDeviceName());
			Session session = sessions.get(uuid);
			WebSocketDevice clientDevice = new WebSocketDevice(uuid);
			conn = new WebSocketConnection(clientDevice, session, connectionId);
		}
		return conn;
	}
	
	@Override
	public ClientConnection openActiveConnection(String uuid) throws NetworkException, IOException {
		Session session = sessions.get(uuid);
		
		WebSocketConnection conn;
		if(session == null){
//			throw new NetworkException(String.format("No Socket available to device %s", uuid));
			Session serverSession = sessions.values().iterator().next();
			String serverUUID = sessionToUUID.values().iterator().next();
			WebSocketDevice clientDevice = new WebSocketDevice(serverUUID);
			conn = new WebSocketConnection(clientDevice, serverSession);
			conn.relay_type = "RREQ";
			conn.from = getAvailableNetworkDevice().getNetworkDeviceName();
			conn.to = uuid;
			System.out.println(conn.getConnectionId()+" relates to "+conn.getClientDevice().getNetworkDeviceName());
		}else{
			WebSocketDevice clientDevice = new WebSocketDevice(uuid);
			conn = new WebSocketConnection(clientDevice, session);
		}
		connections.put(conn.getConnectionId(), conn);
		myOpenedConnections.add(conn.getConnectionId());
		return conn;
	}

//	public void relay(UUID connectionId, String from, String to, String message){
//		if(to.equals(getAvailableNetworkDevice().getNetworkDeviceName())){
////			its me
//			System.out.println("Its me \\o/");
//		}else{
//			Session session = sessions.get(to);
//			WebSocketDevice clientDevice = new WebSocketDevice(to);
//			WebSocketConnection conn;
//			conn = new WebSocketConnection(clientDevice, session);
//			conn.relay = true;
//			conn.from = from;
//			conn.to = to;
//		}
//	}
	
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

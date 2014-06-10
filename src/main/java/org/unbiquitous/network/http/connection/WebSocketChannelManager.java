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
	private Map<String, Session> deviceUUIDToSession = new HashMap<>();
	private Map<String, String> sessionToDeviceUUID = new HashMap<>();
	private Set<UUID> myOpenedConnections = new HashSet<>();

	private Queue<WebSocketDevice> enteredQueue = new ArrayDeque<>();
	private WebSocketRadar radar;
	private boolean relayMode = false;

	public WebSocketChannelManager(ConnectionManagerListener listener) {
		this.listener = listener;
	}

	public void setRelayMode(boolean relayMode) {
		this.relayMode = relayMode;
	}
	
	public ClientConnection openPassiveConnection(String networkDeviceName)
			throws NetworkException, IOException {
		throw new RuntimeException(
				"Passive connections are not supported by websockets");
	}

	public NetworkDevice getAvailableNetworkDevice() {
		return device;
	}

	public void tearDown() throws NetworkException, IOException {
	}

	public void addConnection(String uuid, Session session) {
		deviceUUIDToSession.put(uuid, session);
		sessionToDeviceUUID.put(session.getId(), uuid);
	}

	public void notifyListener(WebSocketConnection conn) {
		UUID connectionID = null;
		connectionID = conn.getConnectionId();
		synchronized (connectionID) {
			if (!myOpenedConnections.contains(conn.getConnectionId())) {
				listener.handleClientConnection(conn);
				myOpenedConnections.remove(conn.getConnectionId());
			}
		}
	}

	public boolean knows(String uuid) {
		return deviceUUIDToSession.containsKey(uuid);
	}

	public void handleIncommingMessage(String message, Session session) {
		int first_point = message.indexOf(":");
		String type = message.substring(0, first_point);
		WebSocketConnection conn = null;
		if (type.equalsIgnoreCase("Hi")) {
			conn = new HiConnection(null, session, this);
			String uuid = ((HiConnection)conn).getUUID(message);
			boolean isNotKnown = !knows(uuid);
			conn.received(message);
			if (relayMode && isNotKnown){
				for(String deviceUUID : deviceUUIDToSession.keySet()){
					if(!deviceUUID.equals(uuid)){
						try {
							Session c = deviceUUIDToSession.get(deviceUUID);
							c.getBasicRemote().sendText("MEET:"+uuid);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		} else if (type.equalsIgnoreCase("Hello")) {
			conn = new HelloConnection(null, session, this);
			conn.received(message);
		} else if (type.equalsIgnoreCase("MEET")) {
			String uuid = message.substring(first_point+1);
			deviceEntered(uuid);
		} else if (type.equalsIgnoreCase("FORGET")) {
			String uuid = message.substring(first_point+1);
			radar.deviceLeft(new WebSocketDevice(uuid));
		} else if (type.equalsIgnoreCase("MSG")) {
			int second_point = message.indexOf(":", first_point + 1);
			String uuid = message.substring(first_point + 1, second_point);
			UUID connectionId = UUID.fromString(uuid);
			// TODO: WTF ??
			conn = this.getConnection(session.getId(), connectionId);
			conn.received(message);
		} else if (type.equalsIgnoreCase("RELAY")) {
			int second_point = message.indexOf(":", first_point + 1);
			int third_point = message.indexOf(":", second_point + 1);
			int fourth_point = message.indexOf(":", third_point + 1);
			String connectionID = message.substring(first_point + 1,
					second_point);
			String fromAddress = message.substring(second_point + 1,
					third_point);
			String toAddress = message.substring(third_point + 1, fourth_point);
			String content = message.substring(fourth_point+1);
			
			
			String currentDevice = getAvailableNetworkDevice()
					.getNetworkDeviceName();

			WebSocketDevice originDevice = new WebSocketDevice(fromAddress);
			WebSocketDevice targetDevice = new WebSocketDevice(toAddress);

			UUID connUUID = UUID.fromString(connectionID);
			if (currentDevice.equals(toAddress) && !connections.containsKey(connUUID)) {
				conn = new IncommingRelayConnection(targetDevice, originDevice,
						session, this, connectionID);
				conn.received(content);
			}else if (currentDevice.equals(toAddress) && connections.containsKey(connUUID)) {
				conn = connections.get(connUUID);
				conn.received(content);
			}else if(connections.containsKey(connUUID)){
				conn = connections.remove(connUUID);
				((MiddleManRelayConnection)conn).toOrigin();
				conn.received(message);
			}else{
				Session originSession = deviceUUIDToSession.get(fromAddress);
				Session targetSession = deviceUUIDToSession.get(toAddress);
				conn = new MiddleManRelayConnection(originDevice, targetDevice,
						originSession, targetSession, this, connectionID);
				connections.put(conn.getConnectionId(), conn);
				conn.received(message);
			}
		}else{
			System.out.println(message);
		}
	}

	private WebSocketConnection getConnection(String sessionId,
			UUID connectionId) {
		String uuid = sessionToDeviceUUID.get(sessionId);

		WebSocketConnection conn = connections.get(connectionId);
		if (conn == null) {
			Session session = deviceUUIDToSession.get(uuid);
			WebSocketDevice clientDevice = new WebSocketDevice(uuid);
			conn = new ClientServerConnection(clientDevice, session,
					connectionId, this);
		}
		return conn;
	}

	@Override
	public ClientConnection openActiveConnection(String uuid)
			throws NetworkException, IOException {
		Session session = deviceUUIDToSession.get(uuid);

		WebSocketConnection conn;
		if (session == null) {
			// throw new
			// NetworkException(String.format("No Socket available to device %s",
			// uuid));
			Session serverSession = deviceUUIDToSession.values().iterator().next();
			WebSocketDevice targetDevice = new WebSocketDevice(uuid);
			conn = new OutGoingRelayConnection(getAvailableNetworkDevice(),
					targetDevice, serverSession, this);
		} else {
			WebSocketDevice clientDevice = new WebSocketDevice(uuid);
			conn = new ClientServerConnection(clientDevice, session,
					UUID.randomUUID(), this);
		}
		connections.put(conn.getConnectionId(), conn);
		myOpenedConnections.add(conn.getConnectionId());
		return conn;
	}

	public void deviceEntered(String uuid) {
		WebSocketDevice device = new WebSocketDevice(uuid);
		if (radar == null) {
			enteredQueue.add(device);
		} else {
			radar.deviceEntered(device);
		}
	}

	public void deviceLeft(Session session) {
		String uuid = sessionToDeviceUUID.get(session.getId());
		radar.deviceLeft(new WebSocketDevice(uuid));
		if (relayMode){
			for(String deviceUUID : deviceUUIDToSession.keySet()){
				if(!deviceUUID.equals(uuid)){
					try {
						Session c = deviceUUIDToSession.get(deviceUUID);
						c.getBasicRemote().sendText("FORGET:"+uuid);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	public void setRadar(WebSocketRadar radar) {
		this.radar = radar;
		while (!enteredQueue.isEmpty()) {
			radar.deviceEntered(enteredQueue.poll());
		}
	}
}

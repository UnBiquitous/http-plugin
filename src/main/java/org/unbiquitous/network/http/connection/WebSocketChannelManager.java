package org.unbiquitous.network.http.connection;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.websocket.Session;

import org.unbiquitous.network.http.WebSocketRadar;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.exceptions.NetworkException;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public class WebSocketChannelManager implements ChannelManager {
	private WebSocketDevice device = new WebSocketDevice();
	private ConnectionManagerListener listener;

	private Map<UUID, WebSocketConnection> connections = new HashMap<UUID, WebSocketConnection>();
	private Map<String, Session> deviceUUIDToSession = new HashMap<String, Session>();
	private Map<String, String> sessionToDeviceUUID = new HashMap<String, String>();
	private Set<UUID> myOpenedConnections = new HashSet<UUID>();

	private Queue<WebSocketDevice> enteredQueue = new ArrayDeque<WebSocketDevice>();
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
		new IncommingHandler(this, relayMode, deviceUUIDToSession, connections, sessionToDeviceUUID)
			.handle(message, session);
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
			relayDeviceLeft(uuid);
		}
	}

	private void relayDeviceLeft(String uuid) {
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

	public void setRadar(WebSocketRadar radar) {
		this.radar = radar;
		while (!enteredQueue.isEmpty()) {
			radar.deviceEntered(enteredQueue.poll());
		}
	}
	
	public WebSocketRadar getRadar() {
		return radar;
	}
}

class IncommingHandler {
	private static final Logger LOGGER = UOSLogging.getLogger();
	
	WebSocketChannelManager channel;
	boolean relayMode;
	Map<String, Session> deviceUUIDToSession;
	Map<UUID, WebSocketConnection> connections;
	Map<String, String> sessionToDeviceUUID;
	
	public IncommingHandler(WebSocketChannelManager channel, boolean relayMode,
			Map<String, Session> deviceUUIDToSession,
			Map<UUID, WebSocketConnection> connections,
			Map<String, String> sessionToDeviceUUID) {
		super();
		this.channel = channel;
		this.relayMode = relayMode;
		this.deviceUUIDToSession = deviceUUIDToSession;
		this.connections = connections;
		this.sessionToDeviceUUID = sessionToDeviceUUID;
	}

	public void handle(String message, Session session){
		int first_point = message.indexOf(":");
		String type = message.substring(0, first_point);
		if (type.equalsIgnoreCase("Hi")) {
			handleHi(message, session);
		} else if (type.equalsIgnoreCase("Hello")) {
			handleHello(message, session);
		} else if (type.equalsIgnoreCase("MEET")) {
			handleMeet(message, first_point);
		} else if (type.equalsIgnoreCase("FORGET")) {
			handleForget(message, first_point);
		} else if (type.equalsIgnoreCase("MSG")) {
			handleMessage(message, session, first_point);
		} else if (type.equalsIgnoreCase("RELAY")) {
			handleRelay(message, session, first_point);
		}else{
			LOGGER.warning("Unkown message: "+message);
		}
	}

	private void handleMessage(String message, Session session, int first_point) {
		int second_point = message.indexOf(":", first_point + 1);
		String uuid = message.substring(first_point + 1, second_point);
		UUID connectionId = UUID.fromString(uuid);
		// TODO: WTF ??
		WebSocketConnection conn = this.getConnection(session.getId(), connectionId);
		conn.received(message);
	}

	private void handleForget(String message, int first_point) {
		String uuid = message.substring(first_point+1);
		channel.getRadar().deviceLeft(new WebSocketDevice(uuid));
	}

	private void handleMeet(String message, int first_point) {
		String uuid = message.substring(first_point+1);
		channel.deviceEntered(uuid);
	}

	private void handleHello(String message, Session session) {
		HelloConnection conn = new HelloConnection(null, session, channel);
		conn.received(message);
	}

	private void handleHi(String message, Session session) {
		HiConnection conn = new HiConnection(null, session, channel);
		String uuid = conn.getUUID(message);
		boolean isNotKnown = !channel.knows(uuid);
		conn.received(message);
		if (relayMode && isNotKnown){
			relayMeet(uuid);
		}
	}

	private void relayMeet(String uuid) {
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

	private WebSocketConnection getConnection(String sessionId,
			UUID connectionId) {
		String uuid = sessionToDeviceUUID.get(sessionId);

		WebSocketConnection conn = connections.get(connectionId);
		if (conn == null) {
			Session session = deviceUUIDToSession.get(uuid);
			WebSocketDevice clientDevice = new WebSocketDevice(uuid);
			conn = new ClientServerConnection(clientDevice, session,
					connectionId, channel);
		}
		return conn;
	}
	
	
	private void handleRelay(String message, Session session, int first_point) {
		int second_point = message.indexOf(":", first_point + 1);
		int third_point = message.indexOf(":", second_point + 1);
		int fourth_point = message.indexOf(":", third_point + 1);
		String connectionID = message.substring(first_point + 1,
				second_point);
		String fromAddress = message.substring(second_point + 1,
				third_point);
		String toAddress = message.substring(third_point + 1, fourth_point);
		String content = message.substring(fourth_point+1);
		
		
		String currentDevice = channel.getAvailableNetworkDevice()
				.getNetworkDeviceName();

		WebSocketDevice originDevice = new WebSocketDevice(fromAddress);
		WebSocketDevice targetDevice = new WebSocketDevice(toAddress);

		UUID connUUID = UUID.fromString(connectionID);
		if (currentDevice.equals(toAddress) && !connections.containsKey(connUUID)) {
			targetRelay(session, connectionID, content, originDevice,
					targetDevice);
		}else if (currentDevice.equals(toAddress) && connections.containsKey(connUUID)) {
			originRelay(content, connUUID);
		}else if(connections.containsKey(connUUID)){
			relayResponse(message, connUUID);
		}else{
			relayRequest(message, connectionID, fromAddress, toAddress,
					originDevice, targetDevice);
		}
	}

	private void relayRequest(String message, String connectionID,
			String fromAddress, String toAddress, WebSocketDevice originDevice,
			WebSocketDevice targetDevice) {
		Session originSession = deviceUUIDToSession.get(fromAddress);
		Session targetSession = deviceUUIDToSession.get(toAddress);
		WebSocketConnection conn = new MiddleManRelayConnection(originDevice, targetDevice,
				originSession, targetSession, channel, connectionID);
		connections.put(conn.getConnectionId(), conn);
		conn.received(message);
	}

	private void relayResponse(String message, UUID connUUID) {
		WebSocketConnection conn = connections.remove(connUUID);
		((MiddleManRelayConnection)conn).toOrigin();
		conn.received(message);
	}

	private void originRelay(String content, UUID connUUID) {
		WebSocketConnection conn = connections.get(connUUID);
		conn.received(content);
	}

	private void targetRelay(Session session, String connectionID,
			String content, WebSocketDevice originDevice,
			WebSocketDevice targetDevice) {
		WebSocketConnection conn = new IncommingRelayConnection(targetDevice, originDevice,
				session, channel, connectionID);
		conn.received(content);
	}
}

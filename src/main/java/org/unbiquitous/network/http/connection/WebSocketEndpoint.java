package org.unbiquitous.network.http.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

@ClientEndpoint
@ServerEndpoint(value = "/" + ClientMode.END_POINT + "/")
public class WebSocketEndpoint {
	private static final Logger LOGGER = UOSLogging.getLogger();

	// Hate singletons, but its impossible Jetty WebSockets
	// don't allow to control how endpoints are created =[
	private WebSocketChannelManager channel;
	private Map<String, String> incommingToOutcommingConnections;

	public WebSocketEndpoint() {
		channel = (WebSocketChannelManager) channelStorage.get()[0];
		incommingToOutcommingConnections = (Map<String, String>) channelStorage.get()[1];
	}

	@OnOpen
	public void onWebSocketConnect(Session sess) {
		LOGGER.finest("Socket Connected: "
				+ ((WebSocketSession) sess).getRemoteAddress().getHostName());
	}


	@OnMessage
	public void onWebSocketText(String message, Session session) {
		try {
			LOGGER.finest(String.format("Handling '%s'", message));
			if (message.startsWith("Hi:")) {
				handleHi(message, session);
			} else if (message.startsWith("Hello:")) {
				handleHello(message, session);
			} else if (message.startsWith("RREQ:")
					|| message.startsWith("RREPLY:")) {
				int first_point = message.indexOf(":");
				int second_point = message.indexOf(":", first_point + 1);
				int third_point = message.indexOf(":", second_point + 1);
				int fourth_point = message.indexOf(":", third_point + 1);

				String relay_type = message.substring(0, first_point);

				String id = message.substring(first_point + 1, second_point);
				UUID connectionID = UUID.fromString(id);

				String from = message.substring(second_point + 1, third_point);
				String to = message.substring(third_point + 1, fourth_point);
				String content = message.substring(fourth_point + 1);
				// WebSocketConnection conn =
				// channel.getConnection(session.getId(), connectionID);
				// writeContentToStream(content, conn);
				// channel.notifyListener(conn);
				System.out.println("Relay connection : " + connectionID);
				System.out.println("at   :"
						+ channel.getAvailableNetworkDevice()
								.getNetworkDeviceName().substring(from.length()-4));
				System.out.println("from : " + from.substring(from.length()-4));
				System.out.println("to   : " + to.substring(from.length()-4));
				System.out.println("with : " + content);
				
				String currentDevice = channel.getAvailableNetworkDevice()
						.getNetworkDeviceName();
				if (from.equals(currentDevice) && relay_type.equals("RREQ")) {
					System.out.println("Going .. Must not happen "+currentDevice);
					// Not t
				}else if (to.equals(currentDevice) && relay_type.equals("RREQ")) {
					System.out.println("Time to process "+currentDevice);
					WebSocketConnection conn = channel.getConnection(
							session.getId(), connectionID);
					conn.relay_type = "RREPLY";
					conn.from = to;
					conn.to = from;
					System.out.println("Conn "+conn.getConnectionId()+" to "+conn.getClientDevice().getNetworkDeviceName());
					writeContentToStream(content, conn);
					channel.notifyListener(conn);
				}else if (to.equals(currentDevice) && relay_type.equals("RREPLY")) {
					System.out.println("Finish line "+currentDevice);
					WebSocketConnection conn = channel.getConnection(
							session.getId(), connectionID);
					conn.relay_type = "END";
					conn.from = to;
					conn.to = from;
					System.out.println("Conn "+conn.getConnectionId()+" to "+conn.getClientDevice().getNetworkDeviceName());
					conn.write(content);
				}else if (!from.equals(currentDevice) && !to.equals(currentDevice)) {
					WebSocketConnection conn;
					if (!incommingToOutcommingConnections.containsKey(connectionID.toString())){
						System.out.println("Middle man going "+currentDevice);
						WebSocketConnection c = channel.getConnection(session.getId(),connectionID);
						System.out.println("Original "+c.getConnectionId()+" to "+c.getClientDevice().getNetworkDeviceName());
						conn = (WebSocketConnection) channel.openActiveConnection(to);
						incommingToOutcommingConnections.put(conn.getConnectionId().toString(), connectionID.toString());
					}else{
						System.out.println("Middle man comming "+currentDevice);
						String newConnId = incommingToOutcommingConnections.get(connectionID.toString());
						conn = channel.getConnection(session.getId(), UUID.fromString(newConnId));
					}
					conn.relay_type = relay_type;
					conn.from = from;
					conn.to = to;
					System.out.println("Conn "+conn.getConnectionId()+" to "+conn.getClientDevice().getNetworkDeviceName());
					DataOutputStream out = conn.getDataOutputStream();
					out.write(content.getBytes());
					out.flush();
				}else{
					System.out.println("Should never happen "+currentDevice);
				}
				
				/*\if (to.equals(channel.getAvailableNetworkDevice()
						.getNetworkDeviceName()) && relay_type.equals("RREPLY")) {
					System.out.println("Its the end \\o/\\o/ " + relay_type);
					WebSocketConnection conn = channel.getConnection(
							session.getId(), connectionID);
					conn.relay_type = "END";
					conn.from = to;
					conn.to = from;
					conn.write(content);

				} else if (to.equals(channel.getAvailableNetworkDevice()
						.getNetworkDeviceName())) {
					System.out.println("Its me \\o/ " + relay_type);
					WebSocketConnection conn = channel.getConnection(
							session.getId(), connectionID);
					conn.relay_type = "RREPLY";
					conn.from = to;
					conn.to = from;
					writeContentToStream(content, conn);
					channel.notifyListener(conn);
				} else if (incommingToOutcommingConnections
						.containsKey(connectionID.toString())) {
					String destConnectionID = incommingToOutcommingConnections
							.get(connectionID.toString());
					System.out.println("relaying back " + destConnectionID
							+ " to " + connectionID.toString());
					WebSocketConnection conn = channel.getConnection(
							session.getId(), UUID.fromString(destConnectionID));
					conn.relay_type = relay_type;
					conn.from = from;
					conn.to = to;
					DataOutputStream out = conn.getDataOutputStream();
					out.write(content.getBytes());
					out.flush();
				} else {
					WebSocketConnection conn = (WebSocketConnection) channel
							.openActiveConnection(to);
					System.out.println("relaying " + connectionID.toString()
							+ " to " + conn.getConnectionId().toString());
					incommingToOutcommingConnections.put(conn.getConnectionId()
							.toString(), connectionID.toString());

					conn.relay_type = relay_type;
					conn.from = from;
					conn.to = to;
					DataOutputStream out = conn.getDataOutputStream();
					out.write(content.getBytes());
					out.flush();
				}*/
			} else {
				handleConnection(message, session);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "", e);
		}
	}

	private void handleConnection(String message, Session session)
			throws IOException {
		UUID connectionID = getConnectionID(message);
		String content = getContent(message);
		WebSocketConnection conn = channel.getConnection(session.getId(),
				connectionID);
		writeContentToStream(content, conn);
		channel.notifyListener(conn);
	}

	private String getContent(String message) {
		int point_ = message.indexOf(":");
		String content = message.substring(point_ + 1);
		return content;
	}

	private UUID getConnectionID(String message) {
		int point = message.indexOf(":");
		String id = message.substring(0, point);
		UUID connectionID = UUID.fromString(id);
		return connectionID;
	}

	private void writeContentToStream(String content, WebSocketConnection conn)
			throws IOException {
		DataInputStream in = conn.getDataInputStream();
		clearTrailingContent(in);
		conn.write(content);
	}

	private void clearTrailingContent(DataInputStream in) throws IOException {
		in.skipBytes(in.available());
	}

	private void handleHello(String message, Session session) {
		String uuid = message.replaceAll("Hello:", "");
		if (!channel.knows(uuid)) {
			addConnection(session, uuid);
			channel.deviceEntered(uuid);
		}
	}

	private void handleHi(String message, Session session) throws IOException {
		String uuid = message.replaceAll("Hi:", "");
		if (!channel.knows(uuid)) {
			addConnection(session, uuid);
			session.getBasicRemote()
					.sendText(
							String.format("Hello:%s", channel
									.getAvailableNetworkDevice()
									.getNetworkDeviceName()));
		}
	}

	private void addConnection(Session session, String uuid) {
		InetSocketAddress remoteAddress = ((WebSocketSession) session)
				.getRemoteAddress();
		String remoteName = remoteAddress.getHostName();
		LOGGER.finer(String.format("Just met with %s at %s", uuid, remoteName));

		channel.addConnection(uuid, session.getId(), session);
	}

	@OnClose
	public void onWebSocketClose(CloseReason reason, Session session) {
		LOGGER.finest("Session " + session.getId() + " Closed: "
				+ reason.getCloseCode());
		channel.deviceLeft(session);
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		LOGGER.severe(cause.getMessage());
	}

	public static void setChannel(WebSocketChannelManager channel) {
		channelStorage.get()[0] = channel;
	}

	private static final InheritableThreadLocal<Object[]> channelStorage = new InheritableThreadLocal<Object[]>() {
		protected Object[] initialValue() {
			return new Object[] { null,  Collections.synchronizedMap(new HashMap<String, String>())};
		}
	};

}

package org.unbiquitous.network.http.connection;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
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

@ClientEndpoint
@ServerEndpoint(value = "/" + ClientMode.END_POINT + "/")
public class WebSocketEndpoint{
	private static final Logger LOGGER = UOSLogging.getLogger();
	
	// Hate singletons, but its impossible Jetty WebSockets 
	// don't allow to control how endpoints are created =[
	private static WebSocketChannelManager channel;
	
	@OnOpen
	public void onWebSocketConnect(Session sess) {
		LOGGER.finest("Socket Connected: " + ((WebSocketSession)sess).getRemoteAddress().getHostName());
	}

	@OnMessage
	public void onWebSocketText(String message, Session session) {
		try {
			LOGGER.finest(String.format("Handling '%s'", message));
			if(message.startsWith("Hi:")){
				handleHi(message, session);
			}else if(message.startsWith("Hello:")){
				handleHello(message, session);
			}else{
				handleConnection(message, session);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,"",e);
		}
	}

	private void handleConnection(String message, Session session)
			throws IOException {
		UUID connectionID = getConnectionID(message);
		String content = getContent(message);
		
		WebSocketConnection conn = channel.getConnection(session.getId(), connectionID);
		writeContentToStream(content, conn);
		channel.notifyListener(conn);
	}

	private String getContent(String message) {
		int point_ = message.indexOf(":");
		String content = message.substring(point_+1);
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
		if (!channel.knows(uuid)){
			addConnection(session, uuid);
		}
	}

	private void handleHi(String message, Session session) throws IOException {
		String uuid = message.replaceAll("Hi:", "");
		if (!channel.knows(uuid)){
			addConnection(session, uuid);
			session.getBasicRemote().sendText(String.format("Hello:%s", channel.getAvailableNetworkDevice().getNetworkDeviceName()));
		}
	}

	private void addConnection(Session session, String uuid) {
		InetSocketAddress remoteAddress = ((WebSocketSession)session).getRemoteAddress();
		String remoteName = remoteAddress.getHostName();
		LOGGER.finer(String.format("Just met with %s at %s", uuid, remoteName));
		
		channel.addConnection(uuid, session.getId(), session);
	}

	@OnClose
	public void onWebSocketClose(CloseReason reason, Session session) {
		LOGGER.finest("Session "+session.getId()+" Closed: " + reason.getCloseCode());
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		LOGGER.severe(cause.getMessage());
	}

	public static void setChannel(WebSocketChannelManager channel) {
		WebSocketEndpoint.channel = channel;
	}

}

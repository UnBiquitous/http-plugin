package org.unbiquitous.network.http.server;

import java.io.DataInputStream;
import java.io.IOException;
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
import org.unbiquitous.network.http.WebSocketChannelManager;
import org.unbiquitous.network.http.WebSocketConnection;
import org.unbiquitous.network.http.WebSocketDevice;
import org.unbiquitous.network.http.client.ClientMode;
import org.unbiquitous.uos.core.UOSLogging;

@ClientEndpoint
@ServerEndpoint(value = "/" + ClientMode.END_POINT + "/")
public class WebServerSocket{
	private static final Logger LOGGER = UOSLogging.getLogger();
	
	private static WebSocketChannelManager channel;
	
	@OnOpen
	public void onWebSocketConnect(Session sess) {
		LOGGER.finest("Socket Connected: " + ((WebSocketSession)sess).getRemoteAddress().getHostName());
	}

	@OnMessage
	public void onWebSocketText(String message, Session session) {
		LOGGER.info("Received TEXT message: " + message+ " with session "+session.getId());
		try {
			if(message.startsWith("Hi:")){
				String uuid = message.replaceAll("Hi:", "");
				if (!channel.knows(uuid)){
					//TODO: this is strange
					WebSocketDevice clientDevice = new WebSocketDevice(uuid);
					WebSocketConnection conn = new WebSocketConnection(clientDevice, session);
					channel.addConnection(uuid, session.getId(), conn);
					session.getBasicRemote().sendText(String.format("Hello:%s", channel.getAvailableNetworkDevice().getNetworkDeviceName()));
				}
			}else if(message.startsWith("Hello:")){
				String uuid = message.replaceAll("Hello:", "");
				if (!channel.knows(uuid)){
					//TODO: this is strange
					WebSocketDevice clientDevice = new WebSocketDevice(uuid);
					WebSocketConnection conn = new WebSocketConnection(clientDevice, session);
					channel.addConnection(uuid, session.getId(), conn);
				}
			}else{
				WebSocketConnection conn = channel.getConnection(session.getId());
				conn.write(message);
				channel.getListener().handleClientConnection(conn);
				DataInputStream in = conn.getDataInputStream();
				in.skipBytes(in.available());
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,"",e);
		}
	}

	@OnClose
	public void onWebSocketClose(CloseReason reason) {
		LOGGER.finest("Socket Closed: " + reason);
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		LOGGER.severe(cause.getMessage());
	}

	public static void setChannel(WebSocketChannelManager channel) {
		WebServerSocket.channel = channel;
	}

}

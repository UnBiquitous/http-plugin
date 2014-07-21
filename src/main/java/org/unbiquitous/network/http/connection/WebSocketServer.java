package org.unbiquitous.network.http.connection;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.unbiquitous.uos.core.UOSLogging;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer {
	private static final Logger LOGGER = UOSLogging.getLogger();

	private WebSocketChannelManager channel;

	public WebSocketServer(int addr) {
		super(new InetSocketAddress(addr));
		channel = channelStorage.get()[0];
	}

	public static void setChannel(WebSocketChannelManager channel) {
		channelStorage.get()[0] = channel;
	}

	private static final InheritableThreadLocal<WebSocketChannelManager[]> channelStorage = new InheritableThreadLocal<WebSocketChannelManager[]>() {
		protected WebSocketChannelManager[] initialValue() {
			return new WebSocketChannelManager[] { null};
		}
	};

	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		InetSocketAddress remote = conn.getRemoteSocketAddress();
		String hostName = remote.getHostName();
		LOGGER.finest("Socket Connected: "+ hostName);
	}

	public void onMessage(WebSocket conn, String message) {
		try {
			LOGGER.finest(String.format("Handling '%s'", message));
			channel.handleIncommingMessage(message, conn);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "", e);
		}
	}

	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		LOGGER.finest("Session " + conn + " Closed: "+ reason);
		channel.deviceLeft(conn);
	}
	
	public void onError(WebSocket conn, Exception ex) {
		LOGGER.log(Level.SEVERE,"WebSocket error." ,ex);
	}
}

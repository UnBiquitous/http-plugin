package org.unbiquitous.network.http.connection;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ServerHandshake;
import org.unbiquitous.uos.core.UOSLogging;

public class WebSocketClient extends org.java_websocket.client.WebSocketClient {
	private static final Logger LOGGER = UOSLogging.getLogger();

	private WebSocketChannelManager channel;

	public WebSocketClient(String url) {
		super(URI.create(url));
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

	public void onOpen(ServerHandshake handshake) {
		WebSocket conn = this.getConnection();
		InetSocketAddress remote = conn.getRemoteSocketAddress();
		String hostName = remote.getHostName();
		LOGGER.finest("Socket Connected: "+ hostName);
	}

	public void onMessage(String message) {
		try {
			LOGGER.finest(String.format("Handling '%s'", message));
			channel.handleIncommingMessage(message, this.getConnection());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "", e);
		}
	}

	public void onClose(int code, String reason, boolean remote) {
		WebSocket conn = this.getConnection();
		LOGGER.finest("Session " + conn + " Closed: "+ reason);
		channel.deviceLeft(conn);
	}
	
	public void onError(Exception ex) {
		LOGGER.log(Level.SEVERE,"WebSocket error." ,ex);
	}
}

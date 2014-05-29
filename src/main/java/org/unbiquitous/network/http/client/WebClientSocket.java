package org.unbiquitous.network.http.client;

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
import org.unbiquitous.network.http.WebSocketConnection;
import org.unbiquitous.uos.core.UOSLogging;

@ClientEndpoint
@ServerEndpoint(value = "/" + ClientMode.END_POINT + "/")
public class WebClientSocket{
	private static final Logger LOGGER = UOSLogging.getLogger();
	
	WebSocketConnection connection;
	
	@OnOpen
	public void onWebSocketConnect(Session sess) {
		LOGGER.finest("Socket Connected: " + ((WebSocketSession)sess).getRemoteAddress().getHostName());
	}

	@OnMessage
	public void onWebSocketText(String message, Session session) {
		LOGGER.info("Received TEXT message: " + message+ " with session "+session.getId());
	}

	@OnClose
	public void onWebSocketClose(CloseReason reason) {
		LOGGER.finest("Socket Closed: " + reason);
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		LOGGER.severe(cause.getMessage());
	}

}

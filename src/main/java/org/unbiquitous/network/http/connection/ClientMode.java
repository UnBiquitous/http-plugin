package org.unbiquitous.network.http.connection;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.eclipse.jetty.util.component.LifeCycle;
import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.model.NetworkDevice;

public class ClientMode implements WebSocketConnectionManager.Mode {
	private static final Logger LOGGER = UOSLogging.getLogger();
	private static final int DEFAULT_IDLE_TIMEOUT = 1*60*1000;
	public static final String END_POINT = "uos_connect";

	private WebSocketContainer container;
	private Session session;
	private URI uri;

	private WebSocketChannelManager channel;

	public void init(InitialProperties props, ConnectionManagerListener listener) throws Exception {
		Integer port = props.getInt("ubiquitos.websocket.port");
		String server = props.getString("ubiquitos.websocket.server");
		if (port == null || server == null) {
			throw new RuntimeException(
					"You must set properties for "
							+ "'ubiquitos.websocket.port' and 'ubiquitos.websocket.server' "
							+ "in order to use WebSocket client mode.");
		}
		setup(server, port);
		channel = new WebSocketChannelManager(listener);
		WebSocketEndpoint.setChannel(channel);
	}

	private void setup(String server, int port) throws Exception {
		String url = String.format("ws://%s:%s/%s/", server, port, END_POINT);
		uri = URI.create(url);

		container = ContainerProvider.getWebSocketContainer();
	}

	public void start() throws Exception {
		int retries = 0;
		while(retries < 10){
			try {
				session = container.connectToServer(WebSocketEndpoint.class, uri);
				retries = 11;
			} catch(ConnectException e){
				LOGGER.warning("Couldn't connect to server. Retrying ...");
				try {Thread.sleep(100);} catch (InterruptedException e1) {}
				retries ++;
			}
		}
		
		session.setMaxIdleTimeout(DEFAULT_IDLE_TIMEOUT);
		
		LOGGER.finest(String.format("This device is %s", channel.getAvailableNetworkDevice().getNetworkDeviceName()));
		WebSocketEndpoint.setChannel(channel);
		
		// TODO: Must happen on every timeout/2
		sendHi();
	}

	private void sendHi() throws IOException {
		NetworkDevice device = channel.getAvailableNetworkDevice();
		String deviceAddr = device.getNetworkDeviceName();
		String welcomeMessage = String.format("Hi:%s",deviceAddr); 
		session.getBasicRemote().sendText(welcomeMessage);
	}

	public void stop() throws Exception {
		if (session != null){
			session.close();
		}
		if (container instanceof LifeCycle) {
			((LifeCycle) container).stop();
		}
	}

	public ChannelManager getChannelManager() {
		return channel;
	}
}

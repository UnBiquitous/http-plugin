package org.unbiquitous.network.http.connection;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.network.http.properties.WebSocketProperties;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.model.NetworkDevice;

public class ClientMode implements WebSocketConnectionManager.Mode {
	private static final Logger LOGGER = UOSLogging.getLogger();
	public static final String END_POINT = "uos_connect";

	private Integer idleTimeout = 5 * 60 * 1000;

	private WebSocketChannelManager channel;
	private boolean running = true;
	private WebSocketClient client;

	public void init(InitialProperties props, ConnectionManagerListener listener)
			throws Exception {
		Properties properties;
		if(!(props instanceof Properties)){
			properties = new Properties(props);
		}else{
			properties =  (Properties) props;
		}
		channel = new WebSocketChannelManager(listener);
		WebSocketClient.setChannel(channel);
		initProperties(properties);
	}

	private void initProperties(Properties props) throws Exception {
		String server = props.getServer();
		if (server == null) {
			throw new RuntimeException("You must set property for "
					+ "'ubiquitos.websocket.server' "
					+ "in order to use WebSocket client mode.");
		}
		Integer port = 8080;
		if (props.getPort() != null) {
			port = props.getPort();
		}
		if (props.getTimeout() != null) {
			idleTimeout = props.getTimeout();
		}
		setup(server, port);
	}

	private void setup(String server, int port) throws Exception {
		String url = String.format("ws://%s:%s/%s/", server, port, END_POINT);
		client = new WebSocketClient(url);
	}

	public void start() throws Exception {
		while (running) {
			connect();
			keepAlive();
		}
	}

	private void keepAlive() throws IOException, InterruptedException {
		try {
			while (running) {
				sendHi();
				Thread.sleep(idleTimeout / 2);
			}
		} catch (WebsocketNotConnectedException e) {
			LOGGER.warning("Problems sending keep alive.");
			LOGGER.log(Level.FINE, "Problems sending keep alive.", e);
		}
	}

	private void connect() throws InterruptedException {
		int retries = 0;
		while (retries < 10) {
			try {
				client.connectBlocking();
//				session.setMaxIdleTimeout(idleTimeout); //TODO: how to manage timeout ??
				retries = 11;
			} catch (WebsocketNotConnectedException e) {
				LOGGER.warning("Couldn't connect to server. Retrying ...");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
				}
				retries++;
			}
		}
	}

	private void sendHi() throws IOException {
		NetworkDevice device = channel.getAvailableNetworkDevice();
		String deviceAddr = device.getNetworkDeviceName();
		String welcomeMessage = String.format("Hi:%s", deviceAddr);
		client.send(welcomeMessage);
	}

	public void stop() throws Exception {
		running = false;
		if (client != null) {
			client.close();
		}
	}

	public WebSocketChannelManager getChannelManager() {
		return channel;
	}

	@SuppressWarnings("serial")
	public static class Properties extends WebSocketProperties {

		public Properties() {
			this(new InitialProperties());
		}

		public Properties(InitialProperties props) {
			super(props);
			put("ubiquitos.websocket.mode", "client");
		}

		public void setServer(String server) {
			put("ubiquitos.websocket.server", server);
		}

		public String getServer() {
			return getString("ubiquitos.websocket.server");
		}
	}
}

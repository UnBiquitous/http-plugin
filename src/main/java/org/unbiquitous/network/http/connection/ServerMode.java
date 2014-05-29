package org.unbiquitous.network.http.connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;

public class ServerMode implements WebSocketConnectionManager.Mode {
	private static final Logger LOGGER = UOSLogging.getLogger();
	private static final int DEFAULT_IDLE_TIMEOUT = 1*60*1000;

	private Server server;
	private Integer port;

	private WebSocketChannelManager channel;

	public void init(InitialProperties props, ConnectionManagerListener listener) {
		this.port = props.getInt("ubiquitos.websocket.port");
		if (port == null ){
			throw new RuntimeException("You must set properties for "
					+ "'ubiquitos.websocket.port' "
					+ "in order to use WebSocket server mode.");
		}
		channel = new WebSocketChannelManager(listener);
	}

	public void start() throws Exception {
		server = createServer(port);
		setEventHandler(createRootServletContext(server));
		startServer();
	}

	private void startServer() throws Exception {
		server.start();
		if(LOGGER.getLevel().intValue() <= Level.FINE.intValue()){
			server.dump(System.err);
		}
		server.join();
	}

	private void setEventHandler(ServletContextHandler context)
			throws DeploymentException {
		ServerContainer wscontainer = WebSocketServerContainerInitializer
				.configureContext(context);
		WebSocketEndpoint.setChannel(channel);
		wscontainer.addEndpoint(WebSocketEndpoint.class);
		LOGGER.finest(String.format("This device is %s", channel.getAvailableNetworkDevice().getNetworkDeviceName()));
	}

	private ServletContextHandler createRootServletContext(Server server) {
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		return context;
	}

	private Server createServer(int port) {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		connector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
		server.addConnector(connector);
		return server;
	}

	public void stop() throws Exception {
		server.stop();
	}

	public ChannelManager getChannelManager() {
		return channel;
	}
}

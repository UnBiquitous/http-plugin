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
import org.unbiquitous.network.http.properties.WebSocketProperties;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;

public class ServerMode implements WebSocketConnectionManager.Mode {
	private static final Logger LOGGER = UOSLogging.getLogger();

	private Server server;
	private Integer port;
	private int idleTimeout = 5*60*1000;

	private WebSocketChannelManager channel;

	public void init(InitialProperties props, ConnectionManagerListener listener) {
		initProperties(new Properties(props));
		channel = new WebSocketChannelManager(listener);
		WebSocketEndpoint.setChannel(channel);
	}

	private void initProperties(Properties props) {
		this.port = props.getPort();
		if (port == null ){
			throw new RuntimeException("You must set properties for "
					+ "'ubiquitos.websocket.port' "
					+ "in order to use WebSocket server mode.");
		}
		if (props.getTimeout() != null){
			idleTimeout = props.getTimeout();
		}
	}

	public void start() throws Exception {
		server = createServer(port);
		setEventHandler(createRootServletContext(server));
		startServer();
	}

	private Server createServer(int port) {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		connector.setIdleTimeout(idleTimeout);
		server.addConnector(connector);
		return server;
	}
	
	private ServletContextHandler createRootServletContext(Server server) {
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		return context;
	}
	
	private void setEventHandler(ServletContextHandler context)
			throws DeploymentException {
		ServerContainer wscontainer = WebSocketServerContainerInitializer
				.configureContext(context);
		WebSocketEndpoint.setChannel(channel);
		wscontainer.addEndpoint(WebSocketEndpoint.class);
		LOGGER.finest(String.format("This device is %s", channel.getAvailableNetworkDevice().getNetworkDeviceName()));
	}

	private void startServer() throws Exception {
		server.start();
		if(LOGGER.getLevel().intValue() <= Level.FINE.intValue()){
			server.dump(System.err);
		}
		server.join();
	}
	
	public void stop() throws Exception {
		if (server != null){
			server.stop();
			server.destroy();
		}
	}

	public WebSocketChannelManager getChannelManager() {
		return channel;
	}
	
	@SuppressWarnings("serial")
	public static class Properties extends WebSocketProperties{
		public Properties() {
			this(new InitialProperties());
		}
		public Properties(InitialProperties props) {
			super(props);
			put("ubiquitos.websocket.mode", "server");
		}
	}
}

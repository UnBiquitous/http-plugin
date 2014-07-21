package org.unbiquitous.network.http.connection;

import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.network.http.properties.WebSocketProperties;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;

public class ServerMode implements WebSocketConnectionManager.Mode {
	private static final int FIVE_MINUTES = 5*60*1000;

	private WebSocketServer server;
	private Integer port = 8080;
	private int idleTimeout = FIVE_MINUTES;
	private boolean relayDevices = false;

	private WebSocketChannelManager channel;

	public void init(InitialProperties props, ConnectionManagerListener listener) {
		Properties properties;
		if(!(props instanceof Properties)){
			properties = new Properties(props);
		}else{
			properties =  (Properties) props;
		}
		initProperties(properties);
		channel = new WebSocketChannelManager(properties, listener);
		channel.setRelayMode(relayDevices);
		WebSocketServer.setChannel(channel);
		
	}

	private void initProperties(Properties props) {
		if (props.getPort() != null ){
			port = props.getPort();
		}
		if (props.getTimeout() != null){
			idleTimeout = props.getTimeout();
		}
		if (props.getRelayDevices() != null){
			relayDevices = props.getRelayDevices();
		}
		
	}

	public void start() throws Exception {
		createNewServer(port);
	}

	private void createNewServer(int port) throws Exception{
		server = new WebSocketServer(port);
		//TODO: set timeout
		server.start();
	}
	
	
	public void stop() throws Exception {
		if (server != null){
			server.stop();
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
		
		public void setRelayDevices(Boolean b) {
			put("ubiquitos.websocket.relayDevices", b);
		}
		
		public Boolean getRelayDevices() {
			return getBool("ubiquitos.websocket.relayDevices");
		}
	}
}

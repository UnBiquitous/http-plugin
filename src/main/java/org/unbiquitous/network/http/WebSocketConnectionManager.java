package org.unbiquitous.network.http;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.unbiquitous.network.http.connection.ClientMode;
import org.unbiquitous.network.http.connection.ServerMode;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.model.NetworkDevice;

public class WebSocketConnectionManager implements ConnectionManager {

	private static final Logger LOGGER = UOSLogging.getLogger();

	private InitialProperties properties;
	
	private Mode mode ;

	public interface Mode {
		void init(InitialProperties properties, ConnectionManagerListener listener) throws Exception;
		void start() throws Exception;
		void stop() throws Exception;
		ChannelManager getChannelManager();
	}
	
	@SuppressWarnings("serial")
	private Map<String, Mode> availableModes = new HashMap<String, WebSocketConnectionManager.Mode>(){{
		put("SERVER",new ServerMode());
		put("CLIENT",new ClientMode());
	}};

	private ConnectionManagerListener listener;

	private ChannelManager channel;
	
	public void init(InitialProperties properties) {
		this.properties = properties;
		try {
			mode = getMode();
			mode.init(properties, listener);
			channel = mode.getChannelManager();
		} catch (Exception e) {
			LOGGER.severe(e.getMessage());
		}
	}
	
	@Override
	public void run() {
		try {
			mode.start();
		} catch (Throwable t) {
			LOGGER.log(Level.SEVERE,t.getMessage(),t);
		}

	}

	private Mode getMode() {
		String modeString = properties.getString("ubiquitos.websocket.mode");
		if(modeString == null || !availableModes.containsKey(modeString.toUpperCase())){
			throw new RuntimeException("To use WebSocketPlugin you mus select a "
					+ "'ubiquitos.websocket.mode' of SERVER or CLIENT");
		}
		return availableModes.get(modeString.toUpperCase());
	}

	public void setConnectionManagerListener(ConnectionManagerListener listener) {
		this.listener = listener;
	}

	public InitialProperties getProperties() {
		return properties;
	}

	public void tearDown() {
		try {
			mode.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public NetworkDevice getNetworkDevice() {
		return channel.getAvailableNetworkDevice();
	}

	public ChannelManager getChannelManager() {
		return channel;
	}
}

	package org.unbiquitous.network.http.connection;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public abstract class WebSocketConnection extends ClientConnection {
	protected WebSocket session;
	protected WebSocketChannelManager channel;
	protected UUID connectionId;
	protected boolean isOpened = true;
	
	public WebSocketConnection(NetworkDevice clientDevice, WebSocket session,
			WebSocketChannelManager channel) {
		this(clientDevice, session, UUID.randomUUID(), channel);
	}

	public WebSocketConnection(NetworkDevice clientDevice, WebSocket session,
			UUID connectionId, WebSocketChannelManager channel) {
		super(clientDevice);
		this.session = session;
		this.connectionId = connectionId;
		this.channel = channel;
	}

	public abstract void received(String content);

	public boolean isConnected() {
		return session.isOpen() && isOpened;
	}

	public UUID getConnectionId() {
		return connectionId;
	}
}

abstract class HiHelloConnection extends WebSocketConnection {
	private static final Logger LOGGER = UOSLogging.getLogger();

	public HiHelloConnection(NetworkDevice clientDevice, WebSocket session,
			WebSocketChannelManager channel) {
		super(clientDevice, session, channel);
	}

	protected String getUUID(String content) {
		int first_point = content.indexOf(":");
		String uuid = content.substring(first_point + 1);
		return uuid;
	}

	protected void addSession(String uuid) {
		InetSocketAddress remoteAddress = session.getRemoteSocketAddress();
		String remoteName = remoteAddress.getHostName();
		LOGGER.finer(String.format("Just met with %s at %s", uuid, remoteName));
		channel.addConnection(uuid, session);
	}

	public DataInputStream getDataInputStream() {
		return null;
	}

	public DataOutputStream getDataOutputStream() {
		return null;
	}

	public void closeConnection() {
		isOpened = false;
	}
}

class HiConnection extends HiHelloConnection {
	public HiConnection(NetworkDevice clientDevice, WebSocket session,
			WebSocketChannelManager channel) {
		super(clientDevice, session, channel);
	}

	@Override
	public void received(String content) {
		try {
			addSession(getUUID(content));
			sendHello();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void sendHello() throws IOException {
		NetworkDevice currentDevice = channel.getAvailableNetworkDevice();
		String currentDeviceUUID = currentDevice.getNetworkDeviceName();
		String helloMessage = String.format("Hello:%s", currentDeviceUUID);
		session.send(helloMessage);
	}
}

class HelloConnection extends HiHelloConnection {
	public HelloConnection(NetworkDevice clientDevice, WebSocket session,
			WebSocketChannelManager channel) {
		super(clientDevice, session, channel);
	}

	@Override
	public void received(String content) {
		String uuid = getUUID(content);
		if (!channel.knows(uuid)) {
			addSession(uuid);
			channel.deviceEntered(uuid);
		}
	}
}

class ClientServerConnection extends WebSocketConnection {
	private static final Logger LOGGER = UOSLogging.getLogger();

	protected OutputStream out;
	protected InputStream in;
	protected PipedOutputStream inWriter;

	public ClientServerConnection(NetworkDevice clientDevice, WebSocket session,
			UUID connectionId, WebSocketChannelManager channel) {
		super(clientDevice, session, connectionId, channel);
		initStreams();
	}

	private void initStreams() {
		try {
			inWriter = new PipedOutputStream();
			in = new PipedInputStream(inWriter);
			out = new ByteArrayOutputSessionStream(this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void received(String content) {
		String message = clearContent(content);
		try {
			DataInputStream in = getDataInputStream();
			clearTrailingContent(in);
			inWriter.write(message.getBytes());
			inWriter.flush();
			channel.notifyListener(this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected String clearContent(String content) {
		int first_point = content.indexOf(":");
		int second_point = content.indexOf(":", first_point + 1);
		String uuid = content.substring(first_point + 1, second_point);
		String message = content.substring(second_point + 1);
		this.connectionId = UUID.fromString(uuid);
		return message;
	}

	protected void clearTrailingContent(DataInputStream in) {
		try {
			in.skipBytes(in.available());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public DataInputStream getDataInputStream() {
		return new DataInputStream(in);
	}

	public DataOutputStream getDataOutputStream() {
		return new DataOutputStream(out);
	}

	public void closeConnection() throws IOException {
		initStreams();
		isOpened = false;
	}

	protected void send(String content) throws IOException {
		if (content != null) {
			content = "MSG:" + connectionId + ":" + content;
			LOGGER.finest(String.format(
					"Flushing content '%s' from Output Stream.", content));
			session.send(content);
		}
	}

	private final class ByteArrayOutputSessionStream extends
			ByteArrayOutputStream {
		private ClientServerConnection conn;

		public ByteArrayOutputSessionStream(ClientServerConnection conn) {
			super();
			this.conn = conn;
		}

		@Override
		public void flush() throws IOException {
			super.flush();
			if (this.size() > 0){
				conn.send(this.toString());
			}
			this.reset();
		}
	}
}

class OutGoingRelayConnection extends ClientServerConnection {
	private static final Logger LOGGER = UOSLogging.getLogger();
	private NetworkDevice originDevice;

	public OutGoingRelayConnection(NetworkDevice originDevice,
			NetworkDevice targetDevice, WebSocket session,
			WebSocketChannelManager channel) {
		super(targetDevice, session, UUID.randomUUID(), channel);
		this.originDevice = originDevice;
	}

	protected void send(String content) throws IOException {
		if (content != null) {
			String fromAddress = originDevice.getNetworkDeviceName();
			String toAddress = clientDevice.getNetworkDeviceName();
			content = String.format("RELAY:%s:%s:%s:%s", connectionId,
					fromAddress, toAddress, content);
			LOGGER.finest(String.format(
					"Flushing content '%s' from Output Stream.", content));
			session.send(content);
		}
	}
	
	@Override
	public void received(String content) {
		String message = clearContent(content);
		try {
			DataInputStream in = getDataInputStream();
			clearTrailingContent(in);
			inWriter.write(message.getBytes());
			inWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected String clearContent(String content) {
		return content;
	}
	
	@Override
	public void closeConnection() throws IOException {
		in.close();
		out.close();
		isOpened = false;
	}
}

class IncommingRelayConnection extends ClientServerConnection {
	private static final Logger LOGGER = UOSLogging.getLogger();
	private NetworkDevice originDevice;

	public IncommingRelayConnection(NetworkDevice originDevice,
			NetworkDevice targetDevice, WebSocket session,
			WebSocketChannelManager channel,
			String connectionID) {
		super(targetDevice, session, UUID.fromString(connectionID), channel);
		this.originDevice = originDevice;
	}

	protected void send(String content) throws IOException {
		if (content != null) {
			String fromAddress = originDevice.getNetworkDeviceName();
			String toAddress = clientDevice.getNetworkDeviceName();
			content = String.format("RELAY:%s:%s:%s:%s", connectionId,
					fromAddress, toAddress, content);
			LOGGER.finest(String.format(
					"Flushing content (Relay) '%s' from Output Stream.", content));
			session.send(content);
		}
	}
	
	protected String clearContent(String content) {
		return content;
	}
}

class MiddleManRelayConnection extends ClientServerConnection {
	private static final Logger LOGGER = UOSLogging.getLogger();
	private NetworkDevice originDevice;
	private WebSocket originSession;

	public MiddleManRelayConnection(NetworkDevice originDevice,
			NetworkDevice targetDevice, WebSocket originSession,
			WebSocket targetSession,
			WebSocketChannelManager channel,
			String connectionID) {
		super(targetDevice, targetSession, UUID.fromString(connectionID), channel);
		this.originDevice = originDevice;
		this.originSession = originSession;
	}

	protected void send(String content) throws IOException {
		if (content != null) {
			String fromAddress = originDevice.getNetworkDeviceName();
			String toAddress = clientDevice.getNetworkDeviceName();
			content = String.format("RELAY:%s:%s:%s:%s", connectionId,
					fromAddress, toAddress, content);
			LOGGER.finest(String.format(
					"Flushing content '%s' from Output Stream.", content));
			session.send(content);
		}
	}

	@Override
	public void received(String content) {
		try {
			session.send(content);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void toOrigin() {
		session=originSession;
	}
}
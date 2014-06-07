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

import javax.websocket.Session;

import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public abstract class WebSocketConnection extends ClientConnection {
	protected Session session;
	protected WebSocketChannelManager channel;
	protected UUID connectionId;
	
	public WebSocketConnection(NetworkDevice clientDevice, Session session, WebSocketChannelManager channel) {
		this(clientDevice, session, UUID.randomUUID(), channel);
	}
	public WebSocketConnection(NetworkDevice clientDevice, Session session, UUID connectionId, WebSocketChannelManager channel) {
		super(clientDevice);
		this.session = session;
		this.connectionId = connectionId;
		this.channel = channel;
	}
	
	
	public abstract void received(String content);
	
	public void send(String content){
		try {
			session.getBasicRemote().sendText(content);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean isConnected() {
		return session.isOpen();
	}

	public UUID getConnectionId() {
		return connectionId;
	}
}

abstract class HiHelloConnection extends WebSocketConnection{
	private static final Logger LOGGER = UOSLogging.getLogger();
	
	public HiHelloConnection(NetworkDevice clientDevice, Session session,
			WebSocketChannelManager channel) {
		super(clientDevice, session, channel);
	}

	protected String getUUID(String content) {
		int first_point = content.indexOf(":");
		String uuid = content.substring(first_point+1);
		return uuid;
	}

	protected void addSession(String uuid) {
		InetSocketAddress remoteAddress = ((WebSocketSession) session)
				.getRemoteAddress();
		String remoteName = remoteAddress.getHostName();
		LOGGER.finer(String.format("Just met with %s at %s", uuid, remoteName));
		channel.addConnection(uuid, session);
	}
	
	public DataInputStream getDataInputStream(){	return null;	}
	public DataOutputStream getDataOutputStream(){	return null;	}
	public void closeConnection(){}
}

class HiConnection extends HiHelloConnection{
	public HiConnection(NetworkDevice clientDevice, Session session,
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
		session.getBasicRemote().sendText(helloMessage);
	}
}

class HelloConnection extends HiHelloConnection{
	public HelloConnection(NetworkDevice clientDevice, Session session,
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

class ClientServerConnection extends WebSocketConnection{
	private static final Logger LOGGER = UOSLogging.getLogger(); 

	protected OutputStream out;
	protected InputStream in;
	protected PipedOutputStream inWriter;
	
	public ClientServerConnection(NetworkDevice clientDevice, Session session, UUID connectionId, WebSocketChannelManager channel) {
		super(clientDevice, session, connectionId, channel);
		initStreams(session);
	}

	private void initStreams(Session session) {
		try {
			inWriter = new PipedOutputStream();
			in = new PipedInputStream(inWriter);
			out = new ByteArrayOutputSessionStream(session);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void received(String content) {
		int first_point = content.indexOf(":");
		int second_point = content.indexOf(":",first_point+1);
		String uuid = content.substring(first_point+1, second_point);
		String message = content.substring(second_point+1);
		System.out.println(message);
		this.connectionId  = UUID.fromString(uuid);
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
	
	private void clearTrailingContent(DataInputStream in){
		try {
			in.skipBytes(in.available());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public DataInputStream getDataInputStream(){
		return new DataInputStream(in);
	}

	public DataOutputStream getDataOutputStream(){
		return new DataOutputStream(out);
	}

	public void closeConnection() throws IOException {
		initStreams(session);
	}

	private final class ByteArrayOutputSessionStream extends
			ByteArrayOutputStream {
		private Session session;
		
		public ByteArrayOutputSessionStream(Session session) {
			super();
			this.session = session;
		}

		@Override
		public void flush() throws IOException {
			super.flush();
			String content = this.toString();
			if (content != null){
				content = "MSG:"+connectionId+":"+content;
				LOGGER.finest(String.format("Flushing content '%s' from Output Stream.", content));
				session.getBasicRemote().sendText(content);
			}
			this.reset();
		}
	}
}
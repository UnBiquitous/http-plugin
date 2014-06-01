package org.unbiquitous.network.http.connection;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;
import java.util.logging.Logger;

import javax.websocket.Session;

import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public class WebSocketConnection extends ClientConnection {
	private static final Logger LOGGER = UOSLogging.getLogger(); 
	
	private Session session;
	private OutputStream out;
	private InputStream in;
	private PipedOutputStream inWriter;
	
	private UUID connectionId;

	public String relay_type;
	public String from;
	public String to;
	
	public WebSocketConnection(NetworkDevice clientDevice, Session session) {
		this(clientDevice, session, UUID.randomUUID());
	}
	
	public WebSocketConnection(NetworkDevice clientDevice, Session session, UUID connectionId) {
		super(clientDevice);
		this.session = session;
		this.connectionId = connectionId;
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

	public void write(String content){
		try {
			inWriter.write(content.getBytes());
			inWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean isConnected() {
		return session.isOpen();
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
				if (relay_type != null){
					content = relay_type+":"+connectionId+":"+from+":"+to+":"+content;
				}else{
					content = connectionId+":"+content;
				}
				LOGGER.finest(String.format("Flushing content '%s' from Output Stream.", content));
				session.getBasicRemote().sendText(content);
			}
			this.reset();
		}
	}

	public UUID getConnectionId() {
		return connectionId;
	}
}
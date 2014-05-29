package uos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;

import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.network.connectionManager.ChannelManager;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerListener;
import org.unbiquitous.uos.core.network.model.connection.ClientConnection;

public class SpikeConnectionManager {
	public static void main(String[] args) throws Exception {
		
		UOSLogging.setLevel(Level.FINEST);
		WebSocketConnectionManager mng = new WebSocketConnectionManager();
		InitialProperties properties = new InitialProperties();
		properties.put("ubiquitos.websocket.port", "8080");
		
		if (args.length > 0){
			clientMode(args, mng, properties);
		}else{
			serverMode(mng, properties);
		}
	}

	private static void serverMode(WebSocketConnectionManager mng,
			InitialProperties properties) {
		properties.put("ubiquitos.websocket.mode", "server");
		
		mng.init(properties);
		mng.setConnectionManagerListener(new ConnectionManagerListener(){
			public void handleClientConnection(ClientConnection clientConnection) {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(clientConnection.getDataInputStream()));
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientConnection.getDataOutputStream()));
					while(reader.ready()){
						String line = reader.readLine();
						System.out.println("Received '"+line+"' now take it back.");
						writer.write(line);
						writer.flush();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			public void tearDown() {}
		});
		mng.run();
		
	}
	
	private static void clientMode(String[] args,
			WebSocketConnectionManager mng, InitialProperties properties)
			throws IOException, InterruptedException {
		properties.put("ubiquitos.websocket.mode", "client");
		properties.put("ubiquitos.websocket.server", args[0]);
		
		mng.init(properties);
		mng.setConnectionManagerListener(new ConnectionManagerListener(){
			public void handleClientConnection(ClientConnection conn) {
				System.out.println("Don't care for "+conn.getClientDevice().getNetworkDeviceName());
				try {
					DataInputStream in = conn.getDataInputStream();
					byte[] buff = new byte[in.available()-2];
					in.read(buff);
					System.out.println("received "+new String(buff));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			public void tearDown() {}
		});
		mng.run();
		
		System.out.println("Who to contact?");
		String dest = System.console().readLine();
		
		
		for (int i = 0 ; i < 1 ; i ++){
			ChannelManager channel = mng.getChannelManager();
			ClientConnection conn = channel.openActiveConnection(dest);
			DataOutputStream out = conn.getDataOutputStream();
			DataInputStream in =  conn.getDataInputStream();
			for(char c : "abacate\ncafé\nchocolate\n".toCharArray()){
				out.write(c);
			}
			out.flush();
			Thread.sleep(100);
			byte[] buff = new byte[in.available()];
			in.read(buff);
			System.out.println(":: "+new String(buff));
			System.out.println("Sleep");
			Thread.sleep(1000);
		}
	}
}

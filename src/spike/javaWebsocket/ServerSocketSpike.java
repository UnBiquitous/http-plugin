package javaWebsocket;

import org.unbiquitous.network.http.connection.WebSocketServer;

public class ServerSocketSpike {

	public static void main(String[] args) {
		new WebSocketServer(8080).start();
	}
}

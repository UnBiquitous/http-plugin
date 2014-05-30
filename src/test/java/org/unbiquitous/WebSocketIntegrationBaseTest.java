package org.unbiquitous;

import org.junit.After;
import org.junit.Before;

public abstract class WebSocketIntegrationBaseTest {
	public static final String PORT = "8080";
	ClientProcess client;
	ServerProcess server;

	@Before public void setup(){
		startServer();
		startClient();
	}

	private ServerProcess startServer() {
		server = new ServerProcess(PORT);
		Thread serverThread = new Thread(server);
		serverThread.start();
		Thread.yield();
		while(!server.finishedInit){
			Thread.yield();
		}
		return server;
	}

	private ClientProcess startClient() {
		client = new ClientProcess(PORT);
		Thread clientThread = new Thread(client);
		clientThread.start();
		Thread.yield();
		while(!client.finishedInit){
			Thread.yield();
		}
		return client;
	}
	
	@After public void teardown(){
		client.uos.tearDown();
		server.uos.tearDown();
	}
}

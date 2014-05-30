package org.unbiquitous;

import org.junit.After;
import org.junit.Before;
import org.unbiquitous.uos.core.UOS;

public abstract class WebSocketIntegrationBaseTest {
	public static final String PORT = "8080";
	UOSProcess client;
	UOSProcess server;

	@Before public void setup(){
		server = startProcess(new ServerProcess(PORT));
		client = startProcess(new ClientProcess(PORT));
	}

	interface UOSProcess extends Runnable{
		public UOS getUos();
		public boolean isInitialized();
	}
	
	private UOSProcess startProcess(UOSProcess process) {
		new Thread(process).start();
		waitForInitialization(process);
		return process;
	}


	private void waitForInitialization(UOSProcess process) {
		Thread.yield();
		while(!process.isInitialized()){
			Thread.yield();
		}
	}

	
	@After public void teardown(){
		client.getUos().tearDown();
		server.getUos().tearDown();
	}
}

package org.unbiquitous;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.unbiquitous.uos.core.UOS;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.driverManager.DriverData;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;

public abstract class WebSocketIntegrationBaseTest {
	public static final String PORT = "8080";
	UOSProcess client;
	UOSProcess server;

	@Before public void setup(){
		server = startProcess(new ServerProcess(PORT));
		client = startProcess(new ClientProcess(PORT));
		
		Thread.yield();
		while(isAlone(server.getUos()) || isAlone(client.getUos())){
			Thread.yield();
		}
	}


	private boolean isAlone(UOS instance) {
		Gateway gateway = instance.getGateway();
		List<UpDevice> devices = gateway.listDevices();
		List<DriverData> drivers = gateway.listDrivers("uos.DeviceDriver");
		return devices.size() < 2 || drivers.size() < 2;
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

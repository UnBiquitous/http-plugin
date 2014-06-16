package org.unbiquitous.network.http;

import java.util.logging.Level;

import org.fest.assertions.core.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.network.http.connection.ServerMode;
import org.unbiquitous.uos.core.UOS;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;

public class RelayDeviceDiscovery extends WebSocketIntegrationBaseTest {

	private UOSProcess client_0;
	private UOSProcess client_1;

	@Before public void setup() { /*Override the default setup*/ 
		UOSLogging.setLevel(Level.FINEST);
	}
	
	@After public void teardown() {
		Thread.yield();
		server.getUos().stop();
		client_0.getUos().stop();
		client_1.getUos().stop();
	};
	
	@SuppressWarnings("rawtypes")
	@Test public void devicesDiscoveryShouldNotBeRelayedWhenNotRequested() throws Exception{
		server = startServer();
		client_0 = startClient(0);
		client_1 = startClient(1);

		Thread.yield();
		while(isAlone(client_0.getUos()) || isAlone(client_1.getUos())){
			Thread.yield();
		}
		
		waitFor(new Condition() {
			public boolean matches(Object arg0) {
				return 	knowsThisNumberOfDevices(server.getUos(), 3) &&
						knowsThisNumberOfDevices(client_0.getUos(), 2) &&
						knowsThisNumberOfDevices(client_1.getUos(), 2);
			}

			private boolean knowsThisNumberOfDevices(UOS uos, int quantity) {
				Gateway gateway = uos.getGateway();
				return gateway.listDevices().size() == quantity;
			}
		}, 20000);
		
	}
	
	
	@SuppressWarnings("rawtypes")
	@Test public void devicesDiscoveryShouldBeRelayedWhenRequested() throws Exception{
		ServerMode.Properties props = new ServerMode.Properties();
		props.put("ubiquitos.uos.deviceName", "server");
		props.setPort(8080);
		props.setTimeout(TIMEOUT);
		props.setRelayDevices(true);
		server = startProcess(new UOSProcess(props));
		client_0 = startClient(0);
		client_1 = startClient(1);

		waitFor(new Condition() {
			public boolean matches(Object arg0) {
				return 	knowsThisNumberOfDevices(server.getUos(), 3) &&
						knowsThisNumberOfDevices(client_0.getUos(), 3) &&
						knowsThisNumberOfDevices(client_1.getUos(), 3);
			}

			private boolean knowsThisNumberOfDevices(UOS uos, int quantity) {
				Gateway gateway = uos.getGateway();
				return gateway.listDevices().size() == quantity;
			}
		}, 4000);
	}
	
	@SuppressWarnings("rawtypes")
	@Test public void devicesLeftShouldBeRelayedWhenRequested() throws Exception{
		ServerMode.Properties props = new ServerMode.Properties();
		props.put("ubiquitos.uos.deviceName", "server");
		props.setPort(8080);
		props.setTimeout(TIMEOUT);
		props.setRelayDevices(true);
		server = startProcess(new UOSProcess(props));
		client_0 = startClient(0);
		client_1 = startClient(1);

		waitFor(new Condition() {
			public boolean matches(Object arg0) {
				return 	knowsThisNumberOfDevices(server.getUos(), 3) &&
						knowsThisNumberOfDevices(client_0.getUos(), 3) &&
						knowsThisNumberOfDevices(client_1.getUos(), 3);
			}

			private boolean knowsThisNumberOfDevices(UOS uos, int quantity) {
				Gateway gateway = uos.getGateway();
				return gateway.listDevices().size() == quantity;
			}
		}, 4000);
		
		client_0.getUos().stop();
		Thread.yield();
		
		waitFor(new Condition() {
			public boolean matches(Object arg0) {
				return 	knowsThisNumberOfDevices(server.getUos(), 2) &&
						knowsThisNumberOfDevices(client_1.getUos(), 2);
			}

			private boolean knowsThisNumberOfDevices(UOS uos, int quantity) {
				Gateway gateway = uos.getGateway();
				return gateway.listDevices().size() == quantity;
			}
		}, 4000);
	}
}

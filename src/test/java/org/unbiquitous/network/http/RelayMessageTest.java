package org.unbiquitous.network.http;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.network.http.util.ClientProcess;
import org.unbiquitous.network.http.util.WebSocketIntegrationBaseTest;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

public class RelayMessageTest extends WebSocketIntegrationBaseTest{

	private UOSProcess client_0;
	private UOSProcess client_1;
	private UOSProcess client_2;

	@Before
	public void setupmoreclients() {
		client_0 = client;
		client_1 = startProcess(new ClientProcess(PORT, TIMEOUT.toString(),1));
		client_2 = startProcess(new ClientProcess(PORT, TIMEOUT.toString(),2));
		
		Thread.yield();
		while(isAlone(client_1.getUos()) || isAlone(client_2.getUos())){
			Thread.yield();
		}
	}
	
	@Test public void relayCallToTheDestinationDevice() throws Exception{
		Gateway gateway_0 = client_0.getUos().getGateway();
		UpDevice device_0 = gateway_0.getCurrentDevice();
		
		Gateway gateway_1 = client_1.getUos().getGateway();
		Call listDrivers = new Call("uos.DeviceDriver", "listDrivers");
		Response drivers = gateway_1.callService(device_0, listDrivers);
		assertThat(drivers).isNotNull();
		assertThat(drivers.getError()).isNull();
	}
}

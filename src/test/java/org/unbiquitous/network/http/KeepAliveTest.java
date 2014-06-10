package org.unbiquitous.network.http;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

public class KeepAliveTest extends WebSocketIntegrationBaseTest{

	@Test public void mustStillBeAliveAfterTimeout() throws Exception{
		Thread.sleep(2*TIMEOUT);
		
		Gateway gateway = client.getUos().getGateway();
		UpDevice targetDevice = server.getUos().getGateway().getCurrentDevice();
		
		Call listDrivers = new Call("uos.DeviceDriver", "listDrivers");
		Response drivers = gateway.callService(targetDevice, listDrivers);
		assertThat(drivers).isNotNull();
		assertThat(drivers.getResponseData()).isNotEmpty();
		assertThat(drivers.getError()).isNull();
	}
	
}

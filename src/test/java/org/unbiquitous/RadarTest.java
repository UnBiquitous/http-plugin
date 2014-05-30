package org.unbiquitous;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;

public class RadarTest extends WebSocketIntegrationBaseTest{

	@Test public void whenConnectedMustStayConnected() throws Exception{
		Thread.sleep(1*1000);
		List<UpDevice> serverDevices = server.uos.getGateway().listDevices();
		assertThat(serverDevices).hasSize(2);
		List<UpDevice> clientDevices = client.uos.getGateway().listDevices();
		assertThat(clientDevices).hasSize(2);
	}
	
}

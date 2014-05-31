package org.unbiquitous;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;

public class RadarTest extends WebSocketIntegrationBaseTest{

	@Test public void whenConnectedMustStayConnected() throws Exception{
		List<UpDevice> serverDevices = server.getUos().getGateway().listDevices();
		assertThat(serverDevices).hasSize(2);
		List<UpDevice> clientDevices = client.getUos().getGateway().listDevices();
		assertThat(clientDevices).hasSize(2);
	}
	
	@Test public void whenOneDeviceLeavesItMustNotify() throws Exception{
		client.getUos().tearDown();
		List<UpDevice> serverDevices = server.getUos().getGateway().listDevices();
		assertThat(serverDevices).hasSize(1);
	}
	
}

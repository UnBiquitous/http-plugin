package org.unbiquitous.network.http;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.network.http.util.WebSocketIntegrationBaseTest;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;

public class RadarTest extends WebSocketIntegrationBaseTest{

	@Before public void setup(){
		UOSLogging.setLevel(Level.FINEST);
		super.setup();
	}
	
	@Test public void whenConnectedMustStayConnected() throws Exception{
		List<UpDevice> serverDevices = server.getUos().getGateway().listDevices();
		assertThat(serverDevices).hasSize(2);
		List<UpDevice> clientDevices = client.getUos().getGateway().listDevices();
		assertThat(clientDevices).hasSize(2);
	}
	
	@Test public void whenOneDeviceLeavesItMustNotify() throws Exception{
		client.getUos().stop();
		List<UpDevice> serverDevices = server.getUos().getGateway().listDevices();
		assertThat(serverDevices).hasSize(1);
	}
	
}

package org.unbiquitous.network.http;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Map;

import org.fest.assertions.core.Condition;
import org.junit.Test;
import org.unbiquitous.network.http.WebSocketIntegrationBaseTest.UOSProcess;
import org.unbiquitous.network.http.connection.ClientMode;
import org.unbiquitous.network.http.connection.ServerMode;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

public class ClientServerServiceCallTest extends WebSocketIntegrationBaseTest {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test public void callASimpleService() throws Exception{
		Gateway gateway = client.getUos().getGateway();
		UpDevice targetDevice = server.getUos().getGateway().getCurrentDevice();
		
		Call listDrivers = new Call("uos.DeviceDriver", "listDrivers");
		Response drivers = gateway.callService(targetDevice, listDrivers);
		Map driverList = (Map) drivers.getResponseData("driverList");
		assertThat(driverList).isNotNull().has(new Condition<Map>() {
			public boolean matches(Map o) {
				return o.size() > 0;
			}
		});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test public void callASimpleServiceFromServer() throws Exception{
		Gateway gateway = server.getUos().getGateway();
		UpDevice targetDevice = client.getUos().getGateway().getCurrentDevice();
		
		Call listDrivers = new Call("uos.DeviceDriver", "listDrivers");
		Response drivers = gateway.callService(targetDevice, listDrivers);
		Map driverList = (Map) drivers.getResponseData("driverList");
		assertThat(driverList).isNotNull().has(new Condition<Map>() {
			public boolean matches(Map o) {
				return o.size() > 0;
			}
		});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test public void callAnestedService() throws Exception{
		Gateway gateway = client.getUos().getGateway();
		UpDevice targetDevice = server.getUos().getGateway().getCurrentDevice();
		
		Call handshake = new Call("uos.DeviceDriver", "handshake");
		handshake.addParameter("device", gateway.getCurrentDevice());
		Response otherGuy = gateway.callService(targetDevice, handshake);
		Map device = (Map) otherGuy.getResponseData("device");
		assertThat(device).isEqualTo(targetDevice.toJSON().toMap());
	}
}

package org.unbiquitous.network.http;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Map;

import org.fest.assertions.core.Condition;
import org.junit.Test;
import org.unbiquitous.network.http.connection.ClientMode;
import org.unbiquitous.network.http.connection.ServerMode;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

public class BigServiceCallsTest extends WebSocketIntegrationBaseTest {
	
	private static final int MAX_BUFFER_SIZE = 2*16*128;

	@SuppressWarnings("rawtypes")
	public void setup() {
		runServer();
		runClient();
		
		waitFor(new Condition() {
			public boolean matches(Object arg0) {
				return !isAlone(server.getUos()) && !isAlone(client.getUos());
			}
		}, 5000);
	}

	private void runServer() {
		ServerMode.Properties props = new ServerMode.Properties();
		props.setDeviceName("server");
		props.setPort(8080);
		props.setTimeout(TIMEOUT);
		props.setMessageBufferSize(MAX_BUFFER_SIZE);
		server = startProcess(new UOSProcess(props));
	};
	
	private void runClient() {
		ClientMode.Properties props_ = new ClientMode.Properties();
		props_.put("ubiquitos.uos.deviceName", "client_0");
		props_.setPort(8080);
		props_.setTimeout(TIMEOUT);
		props_.setServer("localhost");
		props_.setMessageBufferSize(MAX_BUFFER_SIZE);
		client = startProcess(new UOSProcess(props_));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test public void byDefaultServicesShouldSupportMessagesUpToHalfTheBufferSize() throws Exception{
		Gateway gateway = server.getUos().getGateway();
		UpDevice targetDevice = client.getUos().getGateway().getCurrentDevice();
		
		Call listDrivers = new Call("uos.DeviceDriver", "listDrivers");
		listDrivers.addParameter("onekString", createBigParameter(MAX_BUFFER_SIZE/16/2));
		
		Response drivers = gateway.callService(targetDevice, listDrivers);
		Map driverList = (Map) drivers.getResponseData("driverList");
		assertThat(driverList).isNotNull().has(new Condition<Map>() {
			public boolean matches(Map o) {
				return o.size() > 0;
			}
		});
	}

	private StringBuffer createBigParameter(int size) {
		StringBuffer onekString = new StringBuffer();
		for(int i = 0; i < size; i++){
			onekString.append("0123456789ABCDEF");
		}
		return onekString;
	}
}

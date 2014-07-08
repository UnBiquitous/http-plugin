package org.unbiquitous.network.http;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;

import org.fest.assertions.core.Condition;
import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;

public class RadarTest extends WebSocketIntegrationBaseTest{

	@Before public void setup(){
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
		waitFor(new Condition<Void>() {
			public boolean matches(Void arg0) {
				List<UpDevice> serverDevices = server.getUos().getGateway().listDevices();
				return serverDevices.size() == 1;
			};
		}, 4000);
	}
	
	@Test public void radarMustDieWhenAsked(){
		assertThat(numberOfActiveRadars()).isEqualTo(2);
		
		client.getUos().stop();
		waitFor(new Condition<Void>() {
			public boolean matches(Void arg0) {
				return numberOfActiveRadars() == 1;
			};
		}, 4000);
	}

	private int numberOfActiveRadars() {
		Class<?> filterClass = WebSocketRadar.class;
		return numberOfThreadsRunning(filterClass);
	}

	private int numberOfThreadsRunning(Class<?> filterClass) {
		int numberOfRadars = 0;
		for (Thread t : Thread.getAllStackTraces().keySet()){
			for(StackTraceElement s : t.getStackTrace()){
				if(s.getClassName().equals(filterClass.getName())){
					numberOfRadars ++;
				}
			}
		}
		return numberOfRadars;
	}
}

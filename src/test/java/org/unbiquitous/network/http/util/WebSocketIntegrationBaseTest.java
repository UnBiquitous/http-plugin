package org.unbiquitous.network.http.util;

import java.util.Date;
import java.util.List;

import org.fest.assertions.core.Condition;
import org.junit.After;
import org.junit.Before;
import org.unbiquitous.network.http.connection.ClientMode;
import org.unbiquitous.network.http.connection.ServerMode;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOS;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.driverManager.DriverData;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;

public abstract class WebSocketIntegrationBaseTest {
	public static final String PORT = "8080";
	public static final Integer TIMEOUT = 1000;
	protected UOSProcess client;
	protected UOSProcess server;

	public class UOSProcess implements Runnable{
		private UOS uos;
		boolean finishedInit = false;
		private InitialProperties props;

		public UOSProcess(InitialProperties props) {
			this.props = props;
		}
		
		@Override
		public void run() {
			uos = new UOS();
			getUos().start(props);
			finishedInit = true;
		}

		public UOS getUos() {
			return uos;
		}

		public boolean isInitialized() {
			return finishedInit;
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Before public void setup(){
		server = startServer();
		client = startClient(0);
		
		waitFor(new Condition() {
			public boolean matches(Object arg0) {
				return isAlone(server.getUos()) || isAlone(client.getUos());
			}
		}, 2000);
	}

	protected UOSProcess startServer() {
		ServerMode.Properties props = new ServerMode.Properties();
		props.put("ubiquitos.uos.deviceName", "server");
		props.setPort(8080);
		props.setTimeout(TIMEOUT);
		return startProcess(new UOSProcess(props));
	}

	protected UOSProcess startClient(int index) {
		ClientMode.Properties props_ = new ClientMode.Properties();
		props_.put("ubiquitos.uos.deviceName", "client_"+index);
		props_.setPort(8080);
		props_.setTimeout(TIMEOUT);
		props_.setServer("localhost");
		return startProcess(new UOSProcess(props_));
	}
	
	protected UOSProcess startProcess(UOSProcess process) {
		new Thread(process).start();
		waitForInitialization(process);
		return process;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void waitFor(Condition condition, long timeInMillis){
		Thread.yield();
		Date start = new Date();
		while(condition.matches(null)){
			Thread.yield();
			Date now = new Date();
			if (now.getTime() - start.getTime() > timeInMillis){
				throw new AssertionError("Delay was longer than "+timeInMillis+" milliseconds");
			}
		}
	}
	
	protected boolean isAlone(UOS instance) {
		Gateway gateway = instance.getGateway();
		List<UpDevice> devices = gateway.listDevices();
		List<DriverData> drivers = gateway.listDrivers("uos.DeviceDriver");
		return devices.size() < 2 || drivers.size() < 2;
	}

	@SuppressWarnings("rawtypes")
	private void waitForInitialization(final UOSProcess process) {
		waitFor(new Condition() {
			public boolean matches(Object arg0) {
				return !process.isInitialized();
			}
		}, 1000);
	}

	
	@After public void teardown(){
		client.getUos().stop();
		server.getUos().stop();
	}
}

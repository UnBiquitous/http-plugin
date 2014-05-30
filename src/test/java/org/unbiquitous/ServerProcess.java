package org.unbiquitous;

import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.network.http.WebSocketRadar;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOS;

class ServerProcess implements WebSocketIntegrationBaseTest.UOSProcess {
	private UOS uos;
	private String port;
	boolean finishedInit = false;

	public ServerProcess(String port) {
		this.port = port;
	}
	
	@Override
	public void run() {
		InitialProperties properties = new InitialProperties();
		properties.put("ubiquitos.uos.deviceName", "server");
		String connMng = WebSocketConnectionManager.class.getName();
		properties.put("ubiquitos.connectionManager",connMng);
		String radar = WebSocketRadar.class.getName();
		properties.put("ubiquitos.radar", radar+"("+connMng+")");
		properties.put("ubiquitos.websocket.port", port);
		
		properties.put("ubiquitos.websocket.mode", "server");

		uos = new UOS();
		getUos().init(properties);
		
		finishedInit = true;
	}

	public UOS getUos() {
		return uos;
	}

	public boolean isInitialized() {
		return finishedInit;
	}
}
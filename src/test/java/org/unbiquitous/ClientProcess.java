package org.unbiquitous;

import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.network.http.WebSocketRadar;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOS;

class ClientProcess implements WebSocketIntegrationBaseTest.UOSProcess{
	private UOS uos;
	private String port;
	boolean finishedInit = false;

	public ClientProcess(String port) {
		this.port = port;
	}
	
	@Override
	public void run() {
		InitialProperties properties = new InitialProperties();
		properties.put("ubiquitos.uos.deviceName", "client");
		String connMng = WebSocketConnectionManager.class.getName();
		properties.put("ubiquitos.connectionManager",connMng);
		String radar = WebSocketRadar.class.getName();
		properties.put("ubiquitos.radar", radar+"("+connMng+")");
		properties.put("ubiquitos.websocket.port", port);
		
		properties.put("ubiquitos.websocket.mode", "client");
		properties.put("ubiquitos.websocket.server", "localhost");

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
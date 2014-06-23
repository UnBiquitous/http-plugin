package org.unbiquitous.network.http.properties;

import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.network.http.WebSocketRadar;
import org.unbiquitous.uos.core.InitialProperties;

@SuppressWarnings("serial")
public class WebSocketProperties extends InitialProperties{
	
	public WebSocketProperties() {
		this(new InitialProperties());
	}
	
	public WebSocketProperties(InitialProperties props) {
		super(props);
		addConnectionManager(WebSocketConnectionManager.class);
		addRadar(WebSocketRadar.class, WebSocketConnectionManager.class);
	}
	
	public void setTimeout(Integer timeout){
		put("ubiquitos.websocket.timeout", timeout);
	}
	
	public Integer getTimeout(){
		return getInt("ubiquitos.websocket.timeout");
	}
	
	public void setPort(Integer port){
		put("ubiquitos.websocket.port", port);
	}
	
	public Integer getPort(){
		return getInt("ubiquitos.websocket.port");
	}
}
package org.unbiquitous.network.http;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.unbiquitous.network.http.connection.ClientMode;
import org.unbiquitous.network.http.connection.ServerMode;
import org.unbiquitous.uos.core.InitialProperties;

public class WebSocketPropertiesTest {

	WebSocketConnectionManager mng;
	
	@Before public void setup(){
		mng = new WebSocketConnectionManager();
	}
	
	@After public void teardown(){
		mng.tearDown();
	}
	
	@Test(expected=RuntimeException.class) 
	public void rejectApropertyWithoutAmode(){
		mng.init(new InitialProperties());
	}
	
	@Test(expected=RuntimeException.class) 
	public void rejectApropertyWithoutAValidMode(){
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "dontExist");
		mng.init(prop);
	}

	@Test(expected=RuntimeException.class) 
	public void rejectServerModeWithoutAPort(){
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "server");
		mng.init(prop);
	}
	
	@Test(expected=RuntimeException.class) 
	public void rejectServerModeWithoutAValidPort(){
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "server");
		prop.put("ubiquitos.websocket.port", "notaport");
		mng.init(prop);
	}
	
	@Ignore
	@Test 
	public void acceptsAValidServerModeProperty() throws InterruptedException{
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "server");
		prop.put("ubiquitos.websocket.port", "12345");
		mng.init(prop);
	}
	
	@Ignore
	@Test 
	public void acceptsAServerModeProperty() throws InterruptedException{
		ServerMode.Properties prop = new ServerMode.Properties();
		prop.setPort(12345);
		mng.init(prop);
	}
	
	@Test(expected=RuntimeException.class) 
	public void rejectClientModeWithoutAPort(){
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "client");
		mng.init(prop);
	}
	
	@Test(expected=RuntimeException.class) 
	public void rejectClientModeWithoutAValidPort(){
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "client");
		prop.put("ubiquitos.websocket.port", "notaport");
		mng.init(prop);
	}
	
	@Test(expected=RuntimeException.class) 
	public void rejectClientModeWithoutAServer(){
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "client");
		prop.put("ubiquitos.websocket.port", "12345");
		mng.init(prop);
	}
	
	@Ignore
	@Test
	public void acceptsAValidClientProperty() throws InterruptedException{
		InitialProperties prop = new InitialProperties();
		prop.put("ubiquitos.websocket.mode", "client");
		prop.put("ubiquitos.websocket.port", "12345");
		prop.put("ubiquitos.websocket.server", "www.theguy.com");
		mng.init(prop);
	}
	
	@Ignore
	@Test 
	public void acceptsAClientModeProperty() throws InterruptedException{
		ClientMode.Properties prop = new ClientMode.Properties();
		prop.setPort(123456);
		prop.setServer("www.theguy.com");
		mng.init(prop);
	}
}

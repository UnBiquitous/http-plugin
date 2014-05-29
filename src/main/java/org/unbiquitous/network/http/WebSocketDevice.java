package org.unbiquitous.network.http;

import java.util.Date;
import java.util.UUID;

import org.unbiquitous.uos.core.network.model.NetworkDevice;

public class WebSocketDevice extends NetworkDevice {

	private UUID uuid;

	public WebSocketDevice() {
		Date now = new Date();
		long randomLong = (long)(Math.random()*Long.MAX_VALUE);
		uuid = new UUID(now.getTime(), randomLong);
	}
	
	public WebSocketDevice(String uuid){
		try {
			this.uuid = UUID.fromString(uuid);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getNetworkDeviceName() {
		return uuid.toString();
	}

	@Override
	public String getNetworkDeviceType() {
		return "WebSocket";
	}

}

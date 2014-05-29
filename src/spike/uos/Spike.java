package uos;

import java.util.logging.Level;

import org.unbiquitous.network.http.WebSocketConnectionManager;
import org.unbiquitous.network.http.WebSocketRadar;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.UOS;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

public class Spike {
	public static void main(String[] args) throws Exception {

		UOSLogging.setLevel(Level.FINEST);
		InitialProperties properties = new InitialProperties();
		properties.put("ubiquitos.connectionManager",
				WebSocketConnectionManager.class.getName());
		properties.put("ubiquitos.radar", WebSocketRadar.class.getName());
		properties.put("ubiquitos.websocket.port", "8080");

		if (args.length > 0) {
			clientMode(args, properties);
		} else {
			serverMode(properties);
		}
	}

	private static void serverMode(InitialProperties properties) {
		properties.put("ubiquitos.websocket.mode", "server");

		UOS uos = new UOS();
		uos.init(properties);
	}

	private static void clientMode(String[] args, InitialProperties properties)
			throws Exception {
		properties.put("ubiquitos.websocket.mode", "client");
		properties.put("ubiquitos.websocket.server", args[0]);

		UOS uos = new UOS();
		uos.init(properties);

		System.out.println("Who to contact?");
		String dest = System.console().readLine();

		UpDevice device = new UpDevice(dest);
		device.addNetworkInterface(dest, "WebSocket");

		Call listDrivers = new Call("uos.DeviceDriver", "listDrivers");
		Response drivers = uos.getGateway().callService(device, listDrivers);
		System.out.println("It has "+drivers.getResponseData("driverList"));
		
		Call handshake = new Call("uos.DeviceDriver", "handshake");
		handshake.addParameter("device", uos.getGateway().getCurrentDevice());
		Response otherGuy = uos.getGateway().callService(device, handshake);
		System.out.println("It is "+otherGuy.getResponseData("device"));
	}
}

http-plugin
========

This is a [uOS](https://github.com/UnBiquitous/uos_core) plugin for establishing a Smart Space based on a central server using HTTP-WebSockets.


Usage
-----------

You can select the device mode using the property `ubiquitos.websocket.mode`.
By default the port used is the `8080`.

### Setting up a server

Programatically:

```java
	UOS uos = new UOS()
	uos.start(new ServerMode.Properties());
```

Config File:

```properties
ubiquitos.connectionManager=org.unbiquitous.network.http.WebSocketConnectionManager
ubiquitos.radar=org.unbiquitous.network.http.WebSocketRadar(org.unbiquitous.network.http.WebSocketConnectionManager)
ubiquitos.websocket.mode=server
```

You can also configure `ubiquitos.websocket.timeout` setting up the connection timeout.

By default, the Server will keep a one on one connection with each device. Leaving client devices unaware of the existence of others. This is useful if you want the server to be the solely provider of information for these guys.

If you want to all devices to see each other. A relay option (`ubiquitos.websocket.relayDevices`) can be used to set the server to intermediate all connections. Connecting all clients to a single smart space.

### Setting up a client

It's mandatory to inform the server address in order to create a client device.

Programatically:

```java
	UOS uos = new UOS()
	ClientMode.Properties props = new ClientMode.Properties();
	props.setServer("www.my.server.net");
	uos.start(props);
```

Config File:

```properties
ubiquitos.connectionManager=org.unbiquitous.network.http.WebSocketConnectionManager
ubiquitos.radar=org.unbiquitous.network.http.WebSocketRadar(org.unbiquitous.network.http.WebSocketConnectionManager)
ubiquitos.websocket.mode=client
ubiquitos.websocket.server=www.my.server.net
```
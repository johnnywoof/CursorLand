package me.johnnywoof;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CursorLand extends WebSocketServer {

	private static int nextID = 0;
	private static String colonDelimiter = Pattern.quote(":");

	private final ConcurrentHashMap<Integer, Integer> portToID = new ConcurrentHashMap<>();

	public CursorLand(int port) {

		super(new InetSocketAddress(port));

	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

		int port = this.getPortFromSocket(webSocket);
		int id = nextID++;

		System.out.println("New client (" + id + ") connection from " + webSocket.getRemoteSocketAddress().getAddress().toString() + " using port " + port + ".");

		this.portToID.put(port, id);

		this.broadcastMessage(port, "C:" + id);

		for (WebSocket ws : this.connections()) {

			int sp = this.getPortFromSocket(ws);

			if (sp != port) {

				webSocket.send("C:" + this.portToID.get(sp));

			}

		}

	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {

		System.out.println("Client " + webSocket.getRemoteSocketAddress().toString() + " disconnected.");

		int port = this.getPortFromSocket(webSocket);

		if (this.portToID.containsKey(port)) {

			int id = this.portToID.get(port);

			this.broadcastMessage(port, "D:" + id);

			this.portToID.remove(port);

		}

	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {

		if (s != null && !s.isEmpty() && s.contains(":")) {

			String[] data = s.split(colonDelimiter);

			//TODO Maybe an integer check
			int x = Integer.parseInt(data[0]);
			int y = Integer.parseInt(data[1]);

			int port = this.getPortFromSocket(webSocket);

			int clientID = this.portToID.get(port);

			this.broadcastMessage(port, clientID + ":" + x + ":" + y);

		}

	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {

		e.printStackTrace();

	}

	private void broadcastMessage(int excludePort, String message) {

		for (WebSocket ws : this.connections()) {

			if (this.getPortFromSocket(ws) != excludePort) {

				ws.send(message);

			}

		}

	}

	private int getPortFromSocket(WebSocket webSocket) {

		return webSocket.getRemoteSocketAddress().getPort();

	}

	public static void main(String[] args) {

		new CursorLand(15345).start();

		System.out.println("Server started on port 15345.");

		//TODO A clean close system.

	}

}

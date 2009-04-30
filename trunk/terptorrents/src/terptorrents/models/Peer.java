package terptorrents.models;

import java.io.*;
import java.net.*;

import terptorrents.comm.PeerConnection;

public class Peer {
	private final String id;
	private final InetSocketAddress address;

	private PeerConnection connection = null;


	public InetSocketAddress getAddress() {
		return address;
	}

	public Peer(String id, String host, int port) throws IOException {
		this.id = id;
		InetAddress addr = InetAddress.getByName(host);
		this.address = new InetSocketAddress(addr, port);
	}

	public String getId() {
		return id;
	}

	public void setConnection(PeerConnection connection) {
		this.connection = connection;
	}

	public PeerConnection getConnection() {
		return connection;
	}
	
	
	
}

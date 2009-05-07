package terptorrents.models;

import java.io.*;
import java.net.*;

import terptorrents.comm.PeerConnection;

public class Peer {
	private final byte[] id;
	private final InetSocketAddress address;
	private int badCount = 0;

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Peer other = (Peer) obj;
		return address.equals(other.address);
	}

	private PeerConnection connection = null;


	public InetSocketAddress getAddress() {
		return address;
	}

	public Peer(byte[] id, String host, int port) throws IOException {
		this.id = id;
		InetAddress addr = InetAddress.getByName(host);
		this.address = new InetSocketAddress(addr, port);
	}

	public byte[] getId() {
		return id;
	}

	public synchronized void disconnect() {
		setConnection(null);
		badCount++;
	}

	public synchronized void setConnection(PeerConnection connection) {
		this.connection = connection;
	}

	public synchronized PeerConnection getConnection() {
		return connection;
	}

	public synchronized boolean isConnected() {
		return connection != null;
	}

	public boolean isConnectable() {
		return !isConnected() && badCount <= 2;
	}

	@Override
	public String toString() {
		//return new String(id) + "|" + address.toString();
		return address.toString();
	}
}

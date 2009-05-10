package terptorrents.models;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;

public class Peer {
	private byte[] id;
	private InetSocketAddress address;
	private InetSocketAddress listenAddress;

	/**
	 * number of times we have failed to connect, or disconnected from this peer
	 */
	private AtomicInteger disconnectCount = new AtomicInteger();

	/**
	 * number of times we have received a bad piece from this peer
	 */
	private AtomicInteger badCount = new AtomicInteger();


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

	public Peer(byte[] id, String host, int port, boolean outgoing) throws IOException {
		this.id = id;
		InetAddress addr = InetAddress.getByName(host);
		this.address = new InetSocketAddress(addr, port);
		if(outgoing) this.listenAddress = this.address;
	}

	public byte[] getId() {
		return id;
	}

	public void setId(byte[] id) {
		this.id = id;
	}

	public synchronized void disconnect() {
		setConnection(null);
		disconnectCount.incrementAndGet();
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
		return !isConnected() && disconnectCount.get() <= 2 && badCount.get() <= Main.MAX_BAD_PIECES_PER_PEER;
	}

	public synchronized void gotBadPiece() {
		if(!isConnected()) return;
		Main.dprint("Got a bad piece from " + toString());
		if(badCount.incrementAndGet() > Main.MAX_BAD_PIECES_PER_PEER) {
			try {
				connection.close();
			} catch(Exception ex) {
				// already closed.
			}
		}
	}

	/**
	 * Forgive this peer. That is, reset disconnectCount and badCount.
	 */
	public void forgive() {
		disconnectCount.set(0);
		badCount.set(0);
	}

	@Override
	public String toString() {
		//return new String(id) + "|" + address.toString();
		return address.toString();
	}
}

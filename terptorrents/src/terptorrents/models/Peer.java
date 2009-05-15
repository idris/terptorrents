package terptorrents.models;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import terptorrents.comm.PeerConnection;

public class Peer {
	private final byte[] id;
	private final InetAddress address;
	/**
	 * port this peer is listening on. Incoming connections will be -1
	 */
	private int port = -1;
	private Set<Integer> ports = Collections.synchronizedSet(new HashSet<Integer>());
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


	public InetAddress getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
		addPort(port);
	}

	public void addPort(int port) {
		this.ports.add(port);
		if(this.port < 0 && port > 0) this.port = port;
	}

	public void changePort() {
		if(ports.isEmpty()) {
			this.port = -1;
			return;
		}
		ports.remove(this.port);
		for(Integer p: ports) {
			this.port = p;
			break;
		}
		if(this.port < 0) changePort();
	}

	/**
	 * create a peer from an incoming connection (unknown listening port)
	 * @param id
	 * @param host
	 * @throws IOException
	 */
	public Peer(byte[] id, String host) throws IOException {
		this(id, host, -1);
	}

	public Peer(byte[] id, String host, int port) throws IOException {
		this.id = id;
		InetAddress addr = InetAddress.getByName(host);
		this.address = addr;
		//new InetSocketAddress(addr, port);
		addPort(port);
	}

	public byte[] getId() {
		return id;
	}

	public synchronized void disconnect() {
		setConnection(null);
		badCount.incrementAndGet();
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
		return !isConnected() && badCount.get() <= 2;
	}

	/**
	 * Forgive this peer. That is, reset badCount.
	 */
	public void forgive() {
		badCount.set(0);
	}

	@Override
	public String toString() {
		//return new String(id) + "|" + address.toString();
		return address.toString() + ":" + port;
	}
}

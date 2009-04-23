package terptorrents.models;

import java.net.InetSocketAddress;

public class Peer {
	InetSocketAddress address;

	/**
	 * Set the address of this peer using the hostname (or IP address) and port
	 * @param hostname - hostname or IP address
	 * @param port
	 */
	public void setAddress(String hostname, int port) {
		address = new InetSocketAddress(hostname, port);
	}
}

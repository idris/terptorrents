package terptorrents.models;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peer {
	private final byte[] id;
	private final InetAddress address;
	private final int port;

	private boolean am_choking;
	private boolean am_interested;
	private boolean peer_choking;
	private boolean peer_interested;

	public Peer(byte[] id, String address, int port) 
	throws UnknownHostException{
		this.id = id;
		this.port = port;
		this.address = InetAddress.getByName(address);

		am_choking = true;
		am_interested = false;
		peer_choking = true;
		peer_interested = false;
	}

}

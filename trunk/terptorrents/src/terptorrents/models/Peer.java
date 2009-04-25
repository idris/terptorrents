package terptorrents.models;

import java.io.IOException;
import java.net.*;

public class Peer {
	private byte[] id;
	private final Socket clientSocket;
	
	private boolean am_choking;
	private boolean am_interested;
	private boolean peer_choking;
	private boolean peer_interested;

	public Peer(byte[] id, String host, int port) throws IOException{
		this.id = id;			
		InetAddress address = InetAddress.getByName(host);
		clientSocket = new Socket(address, port);
				
		am_choking = true;
		am_interested = false;
		peer_choking = true;
		peer_interested = false;
	}
	
	public Peer(Socket clientSocket){
		assert clientSocket != null;
		this.clientSocket = clientSocket;
		
		am_choking = true;
		am_interested = false;
		peer_choking = true;
		peer_interested = false;
	}
	
	public byte [] getId(){
		return id;
	}
}

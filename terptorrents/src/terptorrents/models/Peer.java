package terptorrents.models;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Peer {
	private final Socket clientSocket;
	private final DataInputStream din;
	private final DataOutputStream dout;
	
	private byte[] id;
	private boolean handshake;
	private boolean haveBitField;
	private boolean amChoking;
	private boolean amInterested;
	private boolean peerChoking;
	private boolean peerInterested;
	private LinkedBlockingQueue<BlockRange> incomingRequest;
	private LinkedBlockingQueue<BlockRange> outgoingRequest;

	public Peer(byte[] id, String host, int port) throws IOException{
		this.id = id;			
		InetAddress address = InetAddress.getByName(host);
		clientSocket = new Socket(address, port);
		din = new DataInputStream(clientSocket.getInputStream());
		dout = new DataOutputStream(clientSocket.getOutputStream());
		incomingRequest = new LinkedBlockingQueue<BlockRange>();
		outgoingRequest = new LinkedBlockingQueue<BlockRange>();
		
		amChoking = true;
		amInterested = false;
		peerChoking = true;
		peerInterested = false;
		handshake = false;
		haveBitField = false;
	}
	
	public Peer(Socket clientSocket) throws IOException{
		assert clientSocket != null;
		this.clientSocket = clientSocket;
		din = new DataInputStream(clientSocket.getInputStream());
		dout = new DataOutputStream(clientSocket.getOutputStream());
		
		amChoking = true;
		amInterested = false;
		peerChoking = true;
		peerInterested = false;
		handshake = false;
		haveBitField = false;
	}
	
	public DataInputStream getInputStream(){
		return din;
	}
	
	public DataOutputStream getOutputStream(){
		return dout;
	}
	
	public boolean haveBitField(){
		return haveBitField;
	}
	
	public void receivedBitField(){
		haveBitField = true;
	}
	
	public boolean getHandshake(){
		return handshake;
	}
	
	public void handshake(){
		handshake = true;
	}
	
	public void sentChoke(){
		amChoking = true;
	}
	
	public void receivedChoke(){
		peerChoking = true;
	}
	
	public void sentUnchoke(){
		amChoking = false;
	}
	
	public void receivedUnchoke(){
		peerChoking = false;
	}
	
	public void sentInsterested(){
		amInterested = true;
	}
	
	public void receivedInsterested(){
		peerInterested = true;
	}
	
	public void sentNotInsterested(){
		amInterested = false;
	}
	
	public void receivedNotInsterested(){
		peerInterested = false;
	}
	
	public boolean canIRequest(){
		return handshake && amInterested && !peerChoking;
	}
	
	public boolean canPeerRequest(){
		return  handshake && peerInterested && !amChoking;
	}
	
	public byte [] getId(){
		return id;
	}
	
	protected void finalize() throws Throwable{
	    try {
	    	din.close();
	    	dout.close();
	        clientSocket.close();
	    } catch (IOException e) {
	    	if(terptorrents.Main.DEBUG)
	    		e.printStackTrace();
		} finally {
	        super.finalize();
	    }
	}
	
}

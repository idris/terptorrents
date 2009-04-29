package terptorrents.comm;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import terptorrents.comm.messages.*;
import terptorrents.models.Peer;


/**
 * 
 * @author idris
 *
 */
public class PeerConnection {
	final Peer peer;
	final Socket socket;

	private static final long MAX_KEEPALIVE = 1000*60*2; // two minutes

	Date lastReceived;
	private boolean choking = true;
	private boolean interested = false;
	private boolean choked = true;
	private boolean interesting = false;
	boolean disconnect = false;

	final LinkedBlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<Message>();
//	final Stack<PieceMessage> outgoingPieces = new Stack<PieceMessage>();
//	final Stack<RequestMessage> incomingRequests = new Stack<RequestMessage>();
//	final Stack<RequestMessage> outgoingRequests = new Stack<RequestMessage>();


	public PeerConnection(Peer peer) throws IOException {
		this.peer = peer;
		socket = new Socket(peer.getAddress().getAddress(), peer.getAddress().getPort());
		lastReceived = new Date();

		HandshakeMessage handshake = new HandshakeMessage();
		// TODO: properly initialize the HandshakeMessage
		outgoingMessages.add(handshake);

		new Thread(new PeerConnectionOut(this)).run();
		new Thread(new PeerConnectionIn(this)).run();
	}

	/**
	 * 
	 * @param peer
	 * @param socket - an already established (and handshaken) socket.
	 * @throws IOException
	 */
	public PeerConnection(Peer peer, Socket socket) throws IOException {
		this.peer = peer;
		this.socket = socket;
		lastReceived = new Date();

		new Thread(new PeerConnectionIn(this)).run();
		new Thread(new PeerConnectionOut(this)).run();
	}


	public void sendMessage(Message message) {
		outgoingMessages.add(message);
/*
		if(message instanceof PieceMessage) {
			outgoingPieces.add((PieceMessage)message);
		} else if(message instanceof RequestMessage) {
			outgoingRequests.add((RequestMessage)message);
		} else {
			outgoingMessages.add(message);
		}
*/
	}

	public Peer getPeer() {
		return peer;
	}

	public void setChoked(boolean choked) {
		this.choked = choked;
	}

	public void setChoking(boolean choking) {
		this.choking = choking;
	}

	public void setInterested(boolean interested) {
		this.interested = interested;
	}

	public void setInteresting(boolean interesting) {
		this.interesting = interesting;
	}

	public boolean amChoking() {
		return choking;
	}

	public boolean amInterested() {
		return interested;
	}

	public boolean peerChoking() {
		return choked;
	}

	public boolean peerInterested() {
		return interesting;
	}

	public boolean canIRequest() {
		return interested && !choked;
	}

	public boolean canPeerRequest() {
		return interesting && !choking;
	}

	boolean peerIsDead() {
		return (System.currentTimeMillis() - lastReceived.getTime()) > MAX_KEEPALIVE;
	}

	void close() {
		try {
			disconnect = true;
			socket.close();
		} catch(IOException ex) {
			peer.setConnection(null);
		}
	}
}

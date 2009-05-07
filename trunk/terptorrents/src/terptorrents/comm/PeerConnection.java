package terptorrents.comm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import metainfo.TorrentParser;

import terptorrents.Main;
import terptorrents.comm.messages.*;
import terptorrents.io.IO;
import terptorrents.models.Peer;
import terptorrents.models.PieceManager;


/**
 * 
 * @author idris
 *
 */
public class PeerConnection {
	final Peer peer;
	final Socket socket;

	private static final long MAX_KEEPALIVE = 1000*60*2; // two minutes
	private static final int CONNECT_TIMEOUT = 1000 * 2;

	volatile Date lastReceived;
	volatile Date lastPieceReceived = null;
	volatile Date lastUnchoked = null;
	volatile double downloadRate = 0;
	volatile double uploadRate = 0;
	private volatile boolean choking = true;
	private volatile boolean interested = false;
	private volatile boolean choked = true;
	private volatile boolean interesting = false;
	volatile boolean disconnect = false;
	boolean handshook = false;

	private final Thread inThread;
	private final Thread outThread;

	final LinkedBlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<Message>();
	//	final Stack<PieceMessage> outgoingPieces = new Stack<PieceMessage>();
	//	final Stack<RequestMessage> incomingRequests = new Stack<RequestMessage>();
	//	final Stack<RequestMessage> outgoingRequests = new Stack<RequestMessage>();


	public PeerConnection(Peer peer) throws IOException {
		this.peer = peer;
		peer.setConnection(this);

		this.socket = new Socket();
		socket.connect(new InetSocketAddress(peer.getAddress().getAddress(), peer.getAddress().getPort()), CONNECT_TIMEOUT);
		lastReceived = new Date();

		sendHandShakeAndBitfield();

		outThread = new Thread(new PeerConnectionOut(this), 
				"OUT_" + peer.getAddress().toString());
		outThread.setDaemon(true);
		outThread.start();

		inThread = new Thread(new PeerConnectionIn(this), 
				"IN_" + peer.getAddress().toString());
		inThread.setDaemon(true);
		inThread.start();
	}

	/**
	 * 
	 * @param peer
	 * @param socket - an already established (and handshaken) socket.
	 * @throws IOException
	 */
	public PeerConnection(Peer peer, Socket socket) throws IOException {
		this.peer = peer;
		peer.setConnection(this);

		this.socket = socket;

		lastReceived = new Date();

		handshook = true;

		sendHandShakeAndBitfield();

		inThread = new Thread(new PeerConnectionIn(this), 
				"IN_" + peer.getAddress().toString());
		inThread.setDaemon(true);
		inThread.start();

		outThread = new Thread(new PeerConnectionOut(this), 
				"OUT_" + peer.getAddress().toString());
		outThread.setDaemon(true);
		outThread.start();
	}

	private void sendHandShakeAndBitfield() {
		HandshakeMessage handshake = new HandshakeMessage(TorrentParser.
				getInstance().getMetaFile().getByteInfoHash(), Main.PEER_ID);
		outgoingMessages.add(handshake);
		if(IO.getInstance().getBitSet().getNumEmptyPieces() 
				!= IO.getInstance().getBitSet().totalNumOfPieces()){
			BitfieldMessage bitfieldMessage = new BitfieldMessage(IO.
					getInstance().getBitSet().getUnsyncBitSet());
			outgoingMessages.add(bitfieldMessage);
		}
	}

	public void sendMessage(Message message) {
		if(disconnect) {
			return;
		}
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

	public void addPeerRequest(RequestMessage requestMessage) {
		// TODO: if they have a different block size, their request may span 2 of our blocks. we should queue ALL blocks between begin and length
		try {
			byte[] data = PieceManager.getInstance().requestBlock(
					requestMessage.getIndex(), requestMessage.getBegin(), 
					requestMessage.getBlockLength());
			sendMessage(new PieceMessage(requestMessage.getIndex(), 
					requestMessage.getBegin(), data));
		} catch(Exception ex) {
			// something went wrong. maybe we didn't have the piece. oh well...
			//ex.printStackTrace();
			Main.dprint("Ignoring. We don't have piece " + requestMessage + 
					" Requested by " + peer.toString());
		}
	}

	public void cancelPeerRequest(CancelMessage msg) {
		for(Message m: outgoingMessages) {
			if(m instanceof PieceMessage) {
				PieceMessage queued = (PieceMessage)m;
				if(queued.getIndex() == msg.getIndex() && queued.getBegin() == msg.getBegin()) {
					outgoingMessages.remove(m);
					break;
				}
			}
		}
	}

	public Peer getPeer() {
		return peer;
	}

	public void setChoked(boolean choked) {
		this.choked = choked;
	}

	public void setChoking(boolean choking) {
		this.choking = choking;
		if(choking == false) {
			lastUnchoked = new Date(System.currentTimeMillis());
		}
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

	public double getDownloadRate() {
		return downloadRate;
	}

	public double getUploadRate() {
		return uploadRate;
	}

	public Date getLastPieceRecievedDate() {
		return lastPieceReceived;
	}

	public Date getLastUnchokedDate() {
		return lastUnchoked;
	}

	boolean peerIsDead() {
		return (System.currentTimeMillis() - lastReceived.getTime()) > MAX_KEEPALIVE;
	}

	public void close() {
		disconnect = true;
	}

	void teardown() {
		try {
			disconnect = true;
			outThread.interrupt();
			socket.close();
		} catch(IOException ex) {

		}
		ConnectionPool.getInstance().removeConnection(this);
		peer.disconnect();
		Main.dprint("Connection to " + peer.toString() + " closed.");
	}

	public String toString() {
		return this.peer.toString();
	}
}

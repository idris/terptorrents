package terptorrents.comm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.BitSet;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
	private static final int CONNECT_TIMEOUT = 1000;

	volatile Date lastReceived;
	volatile Date lastPieceReceived = null;
	volatile Date lastUnchoked = null;
	volatile long bytesDownloaded = 0;
	volatile double timeDownloaded = 0;
	volatile long bytesUploaded = 0;
	volatile double timeUploaded = 0;
//	volatile double downloadRate = 0;
//	volatile double uploadRate = 0;
	volatile long uploadStart = 0;
	volatile long downloadStart = 0;
	private volatile boolean choking = true;
	private volatile boolean interested = false;
	private volatile boolean choked = true;
	private volatile boolean interesting = false;
	volatile boolean disconnect = false;
	boolean handshook = false;
	private volatile boolean dead = false;
	public AtomicInteger outstandingRequests = new AtomicInteger();

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
		socket.connect(
				new InetSocketAddress(peer.getAddress().getAddress(), peer
						.getAddress().getPort()), CONNECT_TIMEOUT);
		lastReceived = new Date();

		sendHandshake();

		outThread = new Thread(new PeerConnectionOut(this), "Active OUT_"
				+ peer.getAddress().toString());
		outThread.setDaemon(true);
		outThread.start();

		inThread = new Thread(new PeerConnectionIn(this), "Active IN_"
				+ peer.getAddress().toString());
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

		sendHandshake();

		inThread = new Thread(new PeerConnectionIn(this), "Passive IN_"
				+ peer.getAddress().toString());
		inThread.setDaemon(true);
		inThread.start();

		outThread = new Thread(new PeerConnectionOut(this), "Passive OUT_"
				+ peer.getAddress().toString());
		outThread.setDaemon(true);
		outThread.start();
	}


	private void sendHandshake() {
		HandshakeMessage handshake = new HandshakeMessage(TorrentParser.
				getInstance().getMetaFile().getByteInfoHash(), Main.PEER_ID);
		outgoingMessages.add(handshake);
	}

	void sendBitfield() {
		if(Main.SUPER_SEEDING_MODE) {
			BitfieldMessage bitfieldMessage=new BitfieldMessage(new BitSet());
			sendMessage(bitfieldMessage);
		} else {
			if(IO.getInstance().getBitSet().getNumEmptyPieces() 
					!= IO.getInstance().getBitSet().totalNumOfPieces()){
				BitfieldMessage bitfieldMessage = new BitfieldMessage(IO.
						getInstance().getBitSet().getUnsyncBitSet());
				sendMessage(bitfieldMessage);
			}
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
		try {
			int begin = requestMessage.getBegin();
			int length = requestMessage.getBlockLength();
			do{
				int lengthToRequest = (length > Main.MAX_REQUEST_BLOCK_SIZE) ? 
						Main.MAX_REQUEST_BLOCK_SIZE : length;
				byte[] data = PieceManager.getInstance().requestBlock(
						requestMessage.getIndex(), begin, lengthToRequest);
				sendMessage(new PieceMessage(requestMessage.getIndex(), 
						begin, data));
				begin += length;
				length -= lengthToRequest;
			}while(length > 0);
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
				if(queued.getIndex() == msg.getIndex() 
						&& queued.getBegin() <= msg.getBegin() 
						&& (queued.getEnd()) >= (msg.getEnd())) {
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

	/**
	 * 
	 * @return download rate for this connection in bytes/second
	 */
	public double getDownloadRate() {
		return (double) bytesDownloaded / timeDownloaded;
	}

	/**
	 * 
	 * @return upload rate for this connection in bytes/second
	 */
	public double getUploadRate() {
		return (double) bytesUploaded / timeUploaded;
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
		if(dead) return;
		dead = true;

		// remove from PieceManager
		PieceManager.getInstance().removePeer(peer);

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
package terptorrents.comm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import terptorrents.Main;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;


/**
 * 
 * @author idris
 *
 */
public class ConnectionPool {
	private static ConnectionPool singleton;

	/**
	 * connections I have initiated
	 */
	private final ArrayBlockingQueue<PeerConnection> outgoingConnections = new ArrayBlockingQueue<PeerConnection>(Main.MAX_PEER_CONNECTIONS);

	/**
	 * connections initiated by other peers
	 */
	private final ArrayBlockingQueue<PeerConnection> incomingConnections = new ArrayBlockingQueue<PeerConnection>(Main.MAX_PEER_CONNECTIONS);


	private ConnectionPool() throws IOException {
		// use newInstance to instantiate this singleton.

		Set<Peer> randomPeers = PeerList.getInstance().getRandomUnconnectedPeers(Main.MAX_PEER_CONNECTIONS);
		for(Peer peer: randomPeers) {
			try {
				outgoingConnections.add(new PeerConnection(peer));
			} catch(IOException ex) {
				// oh well..
			}
		}
	}

	public static ConnectionPool newInstance() throws IOException {
		if (singleton == null) singleton = new ConnectionPool();
		return singleton;
	}

	public static ConnectionPool getInstance() {
		return singleton;
	}

	public void addIncomingConnection(PeerConnection conn) throws InterruptedException {
		incomingConnections.put(conn);
	}

	public synchronized void removeConnection(PeerConnection conn) {
		if(outgoingConnections.remove(conn)) {
			Set<Peer> newPeers = PeerList.getInstance().getRandomUnconnectedPeers(Main.MAX_PEER_CONNECTIONS - outgoingConnections.size());
			for(Peer p: newPeers) {
				try {
					outgoingConnections.add(new PeerConnection(p));
				} catch(IOException ex) {
					PeerList.getInstance().removePeer(p);
				}
			}
		} else {
			incomingConnections.remove(conn);
		}
	}


	/**
	 * 
	 * @return all currently connected connections to peers
	 */
	public ArrayList<PeerConnection> getConnections() {
		ArrayList<PeerConnection> connections = new ArrayList<PeerConnection>();
		connections.addAll(incomingConnections);
		connections.addAll(outgoingConnections);
		return connections;
	}


	/**
	 * 
	 * @return one peer at random that is choked and interested
	 */
	public PeerConnection getPlannedOptimisticUnchokedPeerConnection() {
		List<PeerConnection> PlannedOptimisticUnchokedPeerCandidates = getChokedAndInterested();
		Random rand = new Random();
		return PlannedOptimisticUnchokedPeerCandidates.get(rand.nextInt() %
				PlannedOptimisticUnchokedPeerCandidates.size());
	}

	/**
	 * 
	 * @return all PeerConnections such that each peer is choked and interested
	 */
	private Vector<PeerConnection> getChokedAndInterested() {
		Vector<PeerConnection> list = new Vector<PeerConnection>();
		for(PeerConnection c: getConnections()) {
			if(c.amChoking() && c.peerInterested()) {
				list.add(c);
			}
		}
		return list;
	}

	/**
	 * 
	 * @return all PeerConnections such that each peer is not choking us
	 * also we are interested
	 */
	public Vector<PeerConnection> getNonChokingAndInsterested() {
		Vector<PeerConnection> list = new Vector<PeerConnection>();
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @return all PeerConnections such that each peer is unchoked and interested
	 */
	public Vector<PeerConnection> getUnchokedAndInterested() {
		throw new UnsupportedOperationException();
	}

	/**
	 * each peer is interested and has sent a block in the last 30 seconds
	 * order by fastest download rate
	 * @return
	 */
	public Vector<PeerConnection> getActiveAndInterested() {
		throw new UnsupportedOperationException();
	}

	public Vector<PeerConnection> getUnchoked() {
		throw new UnsupportedOperationException();
	}
	
	public Vector<PeerConnection> getInstersted() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * return all unchoked and interested peers who were unchoked within the last 20 seconds, or who have pending requests
	 * order unchoked peers by last unchoked time, then highest upload rate. order the rest by highest upload rate
	 * @return
	 */
	public Vector<PeerConnection> getSeedableConnections() {
		throw new UnsupportedOperationException();
	}
}

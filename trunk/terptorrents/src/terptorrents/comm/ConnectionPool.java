package terptorrents.comm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Vector;

import terptorrents.models.Peer;
import terptorrents.models.PeerManager;


/**
 * 
 * @author idris
 *
 */
public class ConnectionPool {
	private static final int LISTEN_PORT = 1234;
	private static final int MAX_CONNECTIONS = 40;
	private static final ConnectionPool singleton = new ConnectionPool();

	private PeerListener listener;

	/**
	 * connections I have initiated
	 */
	private final List<PeerConnection> outgoingConnections = new Vector<PeerConnection>(MAX_CONNECTIONS);

	/**
	 * connections initiated by other peers
	 */
	private final List<PeerConnection> incomingConnections = new Vector<PeerConnection>(MAX_CONNECTIONS);


	private ConnectionPool() {
		// use newInstance to instantiate this singleton.

		try {
			listener = new PeerListener(LISTEN_PORT);
		} catch(Exception ex) {
			// FATAL ERROR
		}

		List<Peer> randomPeers = PeerManager.getInstance().getRandomUnconnectedPeers(MAX_CONNECTIONS);
		for(Peer peer: randomPeers) {
			try {
				outgoingConnections.add(new PeerConnection(peer));
			} catch(IOException ex) {
				// oh well..
			}
		}
	}

	public static ConnectionPool getInstance() {
		return singleton;
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
	 * also we are insteresed
	 */
	public Vector<PeerConnection> getNonChokingAndInsterested() {
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

	/**
	 * return all unchoked and interested peers who were unchoked within the last 20 seconds, or who have pending requests
	 * order unchoked peers by last unchoked time, then highest upload rate. order the rest by highest upload rate
	 * @return
	 */
	public PriorityQueue<PeerConnection> getSeedableConnections() {
		throw new UnsupportedOperationException();
	}
}

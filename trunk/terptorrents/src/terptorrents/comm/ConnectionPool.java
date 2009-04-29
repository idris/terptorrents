package terptorrents.comm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
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
	private static ConnectionPool instance;

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
		} catch(IOException ex) {
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

	public static ConnectionPool newInstance() {
		instance = new ConnectionPool();
		return instance;
	}

	public static ConnectionPool getInstance() {
		return instance;
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
		throw new UnsupportedOperationException();
	}


	/**
	 * 
	 * @return all PeerConnections such that each peer is choked and interested
	 */
	public ArrayList<PeerConnection> getChokedAndInterested() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @return all PeerConnections such that each peer is not choking us
	 */
	public ArrayList<PeerConnection> getNonChoking() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @return all PeerConnections such that each peer is unchoked and interested
	 */
	public ArrayList<PeerConnection> getUnchokedAndInterested() {
		throw new UnsupportedOperationException();
	}

	/**
	 * each peer is interested and has sent a block in the last 30 seconds
	 * order by fastest download rate
	 * @return
	 */
	public PriorityQueue<PeerConnection> getActiveAndInterested() {
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

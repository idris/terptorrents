package terptorrents.comm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class ConnectionPool {
	private static final int MAX_CONNECTIONS = 40;
	/**
	 * connections I have initiated
	 */
	private final PeerConnection[] outgoingConnections = new PeerConnection[MAX_CONNECTIONS];

	/**
	 * connections initiated by other peers
	 */
	private final PeerConnection[] incomingConnections = new PeerConnection[MAX_CONNECTIONS];


	/**
	 * 
	 * @return all currently connected connections to peers
	 */
	public ArrayList<PeerConnection> getConnections() {
		ArrayList<PeerConnection> connections = new ArrayList<PeerConnection>();
		connections.addAll(Arrays.asList(incomingConnections));
		connections.addAll(Arrays.asList(outgoingConnections));
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

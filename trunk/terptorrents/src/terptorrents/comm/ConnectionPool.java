package terptorrents.comm;

import java.util.ArrayList;
import java.util.Arrays;

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
}

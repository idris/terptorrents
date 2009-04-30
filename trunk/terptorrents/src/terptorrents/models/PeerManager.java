/**
 * 
 */
package terptorrents.models;

import java.util.List;
import java.util.Vector;

/**
 * @author idris
 *
 */
public class PeerManager {
	private static final PeerManager singleton = new PeerManager();
	private List<Peer> peers = new Vector<Peer>();

	private PeerManager() {}

	public static PeerManager getInstance() {
		return singleton;
	}

	public List<Peer> getRandomUnconnectedPeers(int max) {
		return null;
	}

	public void addPeers(List<Peer> newPeers) {
		peers.addAll(newPeers);
	}
}

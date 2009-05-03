/**
 * 
 */
package terptorrents.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

/**
 * @author idris
 *
 */
public class PeerList {
	private static final PeerList singleton = new PeerList();
	private Vector<Peer> peers = new Vector<Peer>();
	private Map<byte[], Peer> peersById = Collections.synchronizedMap(new Hashtable<byte[], Peer>());
	private final Random random = new Random();

	private PeerList() {}

	public static PeerList getInstance() {
		return singleton;
	}

	public Set<Peer> getRandomUnconnectedPeers(int max) {
		// TODO: fix this method! Probably need to keep 2 lists: 1 with conencted peers, 1 with unconnected peers
		Set<Peer> set = new HashSet<Peer>(max);
		do {
			Peer p = peers.get(random.nextInt(peers.size()));
			if(p.getConnection() == null) {
				set.add(p);
			}
		} while(set.size() < max);

		return set;
	}

	public void addPeers(List<Peer> newPeers) {
		for(Peer p: newPeers) {
			if(!peers.contains(p)) {
				peers.add(p);
				peersById.put(p.getId(), p);
			}
		}
	}

	public void removePeer(Peer p) {
		peers.remove(p);
	}

	public Peer getPeer(byte[] peerId) {
		return peersById.get(peerId);
	}
}

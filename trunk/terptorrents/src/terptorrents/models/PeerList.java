/**
 * 
 */
package terptorrents.models;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author idris
 *
 */
public class PeerList {
	private static final PeerList singleton = new PeerList();

	private final Set<Peer> peers = Collections.synchronizedSet(new HashSet<Peer>());
//	private final Map<byte[], Peer> peersById = Collections.synchronizedMap(new Hashtable<byte[], Peer>());
	private final Map<InetSocketAddress, Peer> peersByAddress = Collections.synchronizedMap(new Hashtable<InetSocketAddress, Peer>());

	private PeerList() {}

	public static PeerList getInstance() {
		return singleton;
	}

	public Set<Peer> getRandomUnconnectedPeers(int max) {
		Set<Peer> set = new HashSet<Peer>(max);
		for(Peer p: peers) {
			if(!p.isConnected()) {
				set.add(p);
			}
			if(set.size() >= max) break;
		}

		return set;
	}

	public void addPeer(Peer p) {
		if(!peers.contains(p)) {
			peers.add(p);
			peersByAddress.put(p.getAddress(), p);
		}
	}

	public void addPeers(List<Peer> newPeers) {
		for(Peer p: newPeers) {
			addPeer(p);
		}
	}

	public void removePeer(Peer p) {
		peers.remove(p);
	}

	public Peer getPeer(InetSocketAddress addr) {
		return peersByAddress.get(addr);
	}
}

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
import java.util.Vector;

/**
 * @author idris
 *
 */
public class PeerList {
	private static final PeerList singleton = new PeerList();

	private final Vector<Peer> peers = new Vector<Peer>();
//	private final Map<byte[], Peer> peersById = Collections.synchronizedMap(new Hashtable<byte[], Peer>());
	private final Map<InetSocketAddress, Peer> peersByAddress = Collections.synchronizedMap(new Hashtable<InetSocketAddress, Peer>());

	private PeerList() {}

	public static PeerList getInstance() {
		return singleton;
	}

	public Set<Peer> getRandomConnectablePeers(int max) {
		Set<Peer> set = new HashSet<Peer>(max);
		for(Peer p: peers) {
			if(p.isConnectable()) {
				set.add(p);
			}
			if(set.size() >= max) break;
		}

		return set;
	}

	/**
	 * @return true if peer added successfully
	 */
	public synchronized boolean addPeer(Peer p) {
		int index = peers.indexOf(p);
		if(index >= 0) {
			peers.get(index).forgive();
		} else {
			peers.add(p);
			peersByAddress.put(p.getAddress(), p);
			return true;
		}

		return false;
	}

	public synchronized void addPeers(List<Peer> newPeers) {
		for(Peer p: newPeers) {
			addPeer(p);
		}
	}

	public synchronized void removePeer(Peer p) {
		peers.remove(p);
		peersByAddress.remove(p.getAddress());
	}

	public synchronized Peer getPeer(InetSocketAddress addr) {
		return peersByAddress.get(addr);
	}
}

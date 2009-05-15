/**
 * 
 */
package terptorrents.models;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import terptorrents.comm.ConnectionPool;

/**
 * @author idris
 *
 */
public class PeerList {
	private static final PeerList singleton = new PeerList();

	private final Vector<Peer> peers = new Vector<Peer>();
//	private final Map<byte[], Peer> peersById = Collections.synchronizedMap(new Hashtable<byte[], Peer>());
	private final Map<InetAddress, Peer> peersByAddress = Collections.synchronizedMap(new Hashtable<InetAddress, Peer>());

	private PeerList() {}

	public static PeerList getInstance() {
		return singleton;
	}

	public Set<Peer> getRandomConnectablePeers() {
		Set<Peer> set = new HashSet<Peer>();
		synchronized(peers) {
			for(Peer p: peers) {
				if(p.isConnectable()) {
					set.add(p);
				}
			}
		}

		return set;
	}

	public Set<Peer> getWellKnownPeers() {
		Set<Peer> set = new HashSet<Peer>();
		synchronized(peers) {
			for(Peer p: peers) {
				if(p.getPort() > 0 && !p.isBad()) {
					set.add(p);
				}
			}
		}

		return set;
	}

	/**
	 * @return true if peer added successfully
	 */
	public synchronized boolean addPeer(Peer p) {
		synchronized(peers) {
			int index = peers.indexOf(p);
			if(index >= 0) {
				Peer existing = peers.get(index);
				existing.addPort(p.getPort());
				existing.forgive();
			} else {
				peers.add(p);
				peersByAddress.put(p.getAddress(), p);
				return true;
			}

			return false;
		}
	}

	public synchronized void addPeers(List<Peer> newPeers) {
		synchronized(peers) {
			for(Peer p: newPeers) {
				addPeer(p);
			}
		}

		ConnectionPool.getInstance().refill();
	}

	public synchronized void removePeer(Peer p) {
		synchronized(peers) {
			peers.remove(p);
			peersByAddress.remove(p.getAddress());
		}
	}

	public synchronized Peer getPeer(InetAddress addr) {
		return peersByAddress.get(addr);
	}

	public Set<Peer> getPeerListSnapshot() {
		synchronized(peers) {
			return new HashSet<Peer>(this.peers);
		}
	}
}

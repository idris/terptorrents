package terptorrents.comm;

import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import terptorrents.Main;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;


/**
 * 
 * @author idris
 *
 */
public class ConnectionPool {
	private static final ConnectionPool singleton = new ConnectionPool();

	/**
	 * connections I have initiated
	 */
	private final Vector<PeerConnection> outgoingConnections = 
		new Vector<PeerConnection>(Main.MAX_PEER_CONNECTIONS);

	/**
	 * connections initiated by other peers
	 */
	private final Vector<PeerConnection> incomingConnections = 
		new Vector<PeerConnection>(Main.MAX_PEER_CONNECTIONS);

	/**
	 * each time we create new connection this lock must be acquired
	 *  to avoid data race
	 */
	public final Lock connectLock = new ReentrantLock();

	private final Semaphore incomingSlots = new Semaphore(Main.MAX_PEER_CONNECTIONS);

	private ConnectionPool() {
		// use newInstance to instantiate this singleton.
	}

	private void connect(Peer peer) {
		if(!peer.isConnectable()) return;
		try {
			outgoingConnections.add(new PeerConnection(peer));
		} catch(IOException ex) {
			Main.dprint("Failed to Connect: " + peer.toString() + ". Reason: " + ex.getMessage());
			// throw it out
			peer.setConnection(null);
			peer.changePort();
			if(peer.getPort() > 0) connect(peer);
			else peer.disconnect();
		}
	}

	public synchronized void refill() {
		connectLock.lock();

		try {
			Set<Peer> randomPeers = PeerList.getInstance().getRandomConnectablePeers();
			for(Peer peer: randomPeers) {
				if(outgoingConnections.size() >= Main.MAX_PEER_CONNECTIONS) break;
				connect(peer);
			}
		} finally {
			connectLock.unlock();
		}
	}

	public static synchronized ConnectionPool getInstance() {
		return singleton;
	}

	public void addIncomingConnection(PeerConnection conn) throws InterruptedException {
		incomingConnections.add(conn);
	}

	public boolean acquireIncomingSlot() throws InterruptedException {
		incomingSlots.acquire();
		return true;
	}

	public void releaseIncomingSlot() {
		incomingSlots.release();
	}

	public synchronized void removeConnection(PeerConnection conn) {
		connectLock.lock();

		try {
			if(outgoingConnections.remove(conn)) {
				refill();
			} else {
				if(incomingConnections.remove(conn)) {
					releaseIncomingSlot();
				}
			}
		} finally {
			connectLock.unlock();
		}
	}

	public synchronized void removeSeeders() {
		for(PeerConnection conn: getConnections()) {
			if(!conn.peerInterested()) {
				conn.close();
				removeConnection(conn);
			}
		}
	}

	/**
	 * 
	 * @return all currently connected connections to peers
	 */
	public Vector<PeerConnection> getConnections() {
		Vector<PeerConnection> connections = new Vector<PeerConnection>();
		connections.addAll(incomingConnections);
		connections.addAll(outgoingConnections);
		return connections;
	}


	/**
	 * 
	 * @return one peer at random that is choked and interested
	 */
	public PeerConnection getPlannedOptimisticUnchokedPeerConnection() {
		Vector<PeerConnection> PlannedOptimisticUnchokedPeerCandidates = 
			getChokedAndInterested();
		if(PlannedOptimisticUnchokedPeerCandidates.isEmpty()) return null;
		Random rand = new Random();
		/* Sergey. nextInteger can return negative value. Checking added */
		int randomPeerConnection = Math.abs(rand.nextInt()) %
				PlannedOptimisticUnchokedPeerCandidates.size();
		return PlannedOptimisticUnchokedPeerCandidates.get(randomPeerConnection);
	}

	/**
	 * 
	 * @return all PeerConnections such that each peer is choked and interested
	 */
	private Vector<PeerConnection> getChokedAndInterested() {
		Vector<PeerConnection> list = new Vector<PeerConnection>();
		for(PeerConnection peerConnection: getConnections()) {
			if(peerConnection != null && 
					peerConnection.amChoking() && 
					peerConnection.peerInterested()) {
				list.add(peerConnection);
			}
		}
		return list;
	}

	/**
	 * this is the Can I request list
	 * @return
	 */
	public Vector<PeerConnection> getNonChokingAndInsterested(){
		Vector<PeerConnection> list = new Vector<PeerConnection>();
		Vector<PeerConnection> allConnections = getConnections();		
		for(PeerConnection peerConnection : allConnections){
			if(peerConnection != null && 
					!peerConnection.peerChoking() && 
					peerConnection.amInterested()){
					list.add(peerConnection);
			}
		}
		return list;
	}
	
	/**
	 * each peer is interested and has sent a block in the last 30 seconds
	 * order by fastest download rate
	 * @return
	 */
	public Vector<PeerConnection> getActiveAndInterested() {
		Vector<PeerConnection> list = new Vector<PeerConnection>();
		Vector<PeerConnection> allConnections = getConnections();		
		for(PeerConnection peerConnection : allConnections){
			if( peerConnection != null 
					&& peerConnection.getLastPieceRecievedDate() != null
					&& peerConnection.peerInterested() && System.currentTimeMillis() - 
						peerConnection.getLastPieceRecievedDate().getTime() 
						< 30000){
					list.add(peerConnection);
			}
		}
		Collections.sort(list, new DownloadSpeedComparator());
		return list;
	}

	/**
	 * get all peers that we have unchoked
	 * @return
	 * 
	 */
	public Vector<PeerConnection> getUnchoked() {
		Vector<PeerConnection> list = new Vector<PeerConnection>();
		Vector<PeerConnection> allConnections = getConnections();
		for(PeerConnection peerConnection: allConnections){
			if(peerConnection != null && !peerConnection.amChoking())
				list.add(peerConnection);
		}
		return list;
	}

	/**
	 * get all peers that are interested in us
	 * @return
	 */
	public Vector<PeerConnection> getInstersted() {		
		Vector<PeerConnection> list = new Vector<PeerConnection>();
		Vector<PeerConnection> allConnections = getConnections();		
		for(PeerConnection peerConnection: allConnections){
			if(peerConnection != null && peerConnection.peerInterested())
				list.add(peerConnection);
		}
		return list;
	}

	/**
	 * return all unchoked and interested peers who were unchoked within the 
	 * last 20 seconds, or who have pending requests
	 * order unchoked peers by last unchoked time, then highest upload rate. 
	 * order the rest by highest upload rate
	 * @return
	 */
	public Vector<PeerConnection> getSeedableConnections() {
		Vector<PeerConnection> recentlyUnchoked = new Vector<PeerConnection>();
		Vector<PeerConnection> notRecentlyUnchoked = new Vector<PeerConnection>();
		
		Vector<PeerConnection> allConnections = getConnections();		
		for(PeerConnection peerConnection: allConnections){
			/* if peer has never been unchoked the data is null */
			if(peerConnection != null && peerConnection.getLastUnchokedDate() == null) continue;
			if(System.currentTimeMillis() - peerConnection.
					getLastUnchokedDate().getTime() < 20000){
				recentlyUnchoked.add(peerConnection);
			}else{
				notRecentlyUnchoked.add(peerConnection);
			}
		}
		Collections.sort(recentlyUnchoked, new UploadSpeedComparator());
		Collections.sort(notRecentlyUnchoked, new UploadSpeedComparator());
		recentlyUnchoked.addAll(notRecentlyUnchoked);
		return recentlyUnchoked;
	}
}

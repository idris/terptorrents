package terptorrents.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.RequestMessage;
import terptorrents.exceptions.TerptorrentsModelsCanNotRequstFromThisPeer;
import terptorrents.io.IO;

public class RequestManager implements Runnable {
	private static final RequestManager singleton = new RequestManager();
	private Set<BlockRange> dogpile = Collections.
	synchronizedSet(new HashSet<BlockRange>());

	public static RequestManager getInstance() {
		return singleton;
	}

	public void run() {
		while(true) {
			try {
				Thread.sleep(
						Main.TIME_BETWEEN_RETRANSMITION_OF_UNREPLIED_REQUEST_MESSAGES);
			} catch (InterruptedException e) {
			}
			dogpile.clear();
		}
	}

	/**
	 * 
	 * @param peer
	 * @param numBlockRanges
	 * @return
	 */
	public synchronized void requestBlocks(Peer peer, int numBlocks) 
	throws TerptorrentsModelsCanNotRequstFromThisPeer {
		if(IO.getInstance().isComplete()) return;

		PieceManager.getInstance().blocksLock.lock();

		try {
			PeerConnection conn = peer.getConnection();
			if(conn == null) 
				throw new TerptorrentsModelsCanNotRequstFromThisPeer
				("Disconnected Peer");

			if(conn.peerChoking() || !conn.amInterested())
				throw new TerptorrentsModelsCanNotRequstFromThisPeer(
				"Peer is choking us or We are not intersted");

			boolean dogpileEmpty = dogpile.isEmpty();
			Vector<BlockRange> blockRanges = PieceManager.getInstance().
			getBlockRangeToRequest(peer, new HashSet<BlockRange>(dogpile), 
					numBlocks);
			Main.iprint("Requesting " + blockRanges.size() + " blocks");

			if(blockRanges.isEmpty()) {
				// we got stuck. clear the dogpile and try again.
				if(dogpileEmpty) {
					Main.dprint("=============== FATAL ERROR: nothing left to request! ============");
					throw new TerptorrentsModelsCanNotRequstFromThisPeer("FATAL ERROR: nothing left to request!");
				}
				dogpile.clear();
				requestBlocks(conn.getPeer(), Main.MAX_OUTSTANDING_REQUESTS - conn.outstandingRequests.get());
			} else {
				for(BlockRange br : blockRanges) {
					dogpile.add(br);
					conn.sendMessage(new RequestMessage(br));
				}
			}
		} finally {
			PieceManager.getInstance().blocksLock.unlock();
		}
	}
}

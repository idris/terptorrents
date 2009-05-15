package terptorrents.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

		PeerConnection conn = peer.getConnection();
		if(conn == null) 
			throw new TerptorrentsModelsCanNotRequstFromThisPeer
			("Disconnected Peer");

		if(conn.peerChoking() || !conn.amInterested())
			throw new TerptorrentsModelsCanNotRequstFromThisPeer(
					"Peer is choking us or We are not intersted");

		List<BlockRange> dogpileList = new ArrayList<BlockRange>(dogpile);
		Vector<BlockRange> blockRanges = PieceManager.getInstance().
		getBlockRangeToRequest(peer, dogpileList, 
				numBlocks);
		Main.iprint("Requesting " + blockRanges.size() + " blocks");

		if(blockRanges.isEmpty()) {
			// we got stuck. clear the dogpile and try again.
			if(dogpileList.isEmpty()) {
				if (!IO.getInstance().isComplete()) {
//					PieceManager.reinitialize();
					/* if file is not complete and blockRanges with dogpile
					 * is empty, reinitialize PieceManager
					 */
				} else return;
				throw new TerptorrentsModelsCanNotRequstFromThisPeer("Done requesting from this peer.");
			}
			dogpile.clear();
			int toRequest = Main.MAX_OUTSTANDING_REQUESTS - conn.outstandingRequests.get();
			if(toRequest > 0) {
				requestBlocks(conn.getPeer(), toRequest);
			}
		} else {
			for(BlockRange br : blockRanges) {
				dogpile.add(br);
				conn.sendMessage(new RequestMessage(br));
			}
		}
	}

	public void pieceComplete(int index) {
		// clear out this piece's request from the dogpile (not necessary, but keeps the dogpile clean.
		try {
			Iterator<BlockRange> it = dogpile.iterator();
			while(it.hasNext()) {
				if(it.next().getPieceIndex() == index) {
					it.remove();
				}
			}
		} catch(Exception ex) {
			Main.iprint("EXCEPTION CAUGHT in RequestManager.pieceComplete(): " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
			// no problem
		}
	}
}

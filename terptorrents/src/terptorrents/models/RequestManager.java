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

		Vector<BlockRange> blockRanges = PieceManager.getInstance().
		getBlockRangeToRequest(peer, new HashSet<BlockRange>(dogpile), 
				numBlocks);
		for(BlockRange br : blockRanges) {
			PeerConnection conn = peer.getConnection();
			dogpile.add(br);
			conn.sendMessage(new RequestMessage(br));
		}
	}
}

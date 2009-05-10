package terptorrents.comm.messages;

import java.util.List;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;
import terptorrents.models.BlockRange;
import terptorrents.models.PieceManager;

public class UnchokeMessage extends AbstractMessage {
	@Override
	public int getId() {
		return 1;
	}

	@Override
	public void onSend(PeerConnection conn) {
		conn.setChoking(false);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.setChoked(false);
		List<BlockRange> ranges = PieceManager.getInstance().getBlockRangeToRequestSamePiecePerPeer(conn.getPeer(), Main.MAX_OUTSTANDING_REQUESTS);
		if(ranges.isEmpty()) Main.dprint("NOTHING TO REQUEST FROM  " + conn.getPeer().toString());
		for(BlockRange range: ranges) {
			conn.sendMessage(new RequestMessage(range));
		}
	}
}

package terptorrents.comm.messages;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;
import terptorrents.exceptions.TerptorrentsModelsCanNotRequstFromThisPeer;
import terptorrents.models.RequestManager;

public class UnchokeMessage extends AbstractMessage {
	@Override
	public int getId() {
		return 1;
	}

	@Override
	public void onSend(PeerConnection conn) {
//		conn.setChoking(false);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.setChoked(false);
		try {
			RequestManager.getInstance().requestBlocks(conn.getPeer(), Main.MAX_OUTSTANDING_REQUESTS);
		} catch(TerptorrentsModelsCanNotRequstFromThisPeer ex) {
			Main.dprint("Can not request from this peer EXCEPTION IS CAUGHT: " + conn.getPeer().toString());
		}
	}
}

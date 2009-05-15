package terptorrents.comm.messages;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;
import terptorrents.models.RequestManager;

public class InterestedMessage extends AbstractMessage {

	@Override
	public int getId() {
		return 2;
	}

	@Override
	public void onSend(PeerConnection conn) {
		int toRequest = Main.MAX_OUTSTANDING_REQUESTS - conn.outstandingRequests.get();
		if(conn.canIRequest() && toRequest >= Main.MAX_OUTSTANDING_REQUESTS/2) {
			try {
				RequestManager.getInstance().requestBlocks(conn.getPeer(), toRequest);
			} catch(Exception ex) {
				Main.iprint("Exception in InterestedMessage.onSend() is caught: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
			}
		}

//		conn.setInterested(true);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.setInteresting(true);
	}
}

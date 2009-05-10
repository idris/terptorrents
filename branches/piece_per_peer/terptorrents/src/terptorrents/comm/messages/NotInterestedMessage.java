package terptorrents.comm.messages;

import terptorrents.comm.PeerConnection;

public class NotInterestedMessage extends AbstractMessage {

	@Override
	public int getId() {
		return 3;
	}

	@Override
	public void onSend(PeerConnection conn) {
		conn.setInterested(false);
		if(!conn.peerInterested()) {
			conn.close();
		}
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.setInteresting(false);
		if(!conn.amInterested()) {
			conn.close();
		}
	}
}

package terptorrents.comm.messages;

import terptorrents.comm.PeerConnection;

public class NotInterestedMessage extends Message {

	@Override
	public int getId() {
		return 3;
	}

	@Override
	public void onSend(PeerConnection conn) {
		conn.setInterested(false);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.setInteresting(false);
	}
}

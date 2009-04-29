package terptorrents.comm.messages;

import terptorrents.comm.PeerConnection;

public class InterestedMessage extends Message {

	@Override
	public int getId() {
		return 2;
	}

	@Override
	public void onSend(PeerConnection conn) {
		conn.setInterested(true);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.setInteresting(true);
	}
}

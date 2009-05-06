package terptorrents.comm.messages;

import terptorrents.comm.PeerConnection;

public class ChokeMessage extends AbstractMessage {
	@Override
	public int getId() {
		return 0;
	}

	@Override
	public void onSend(PeerConnection conn) {
		conn.setChoking(true);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.setChoked(true);
	}
}

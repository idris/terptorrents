package terptorrents.comm.messages;

import terptorrents.comm.PeerConnection;
import terptorrents.io.IO;

public class UnchokeMessage extends Message {
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
	}
}

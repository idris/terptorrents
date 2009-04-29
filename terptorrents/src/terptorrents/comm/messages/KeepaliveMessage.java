package terptorrents.comm.messages;

import terptorrents.comm.PeerConnection;

public class KeepaliveMessage extends Message {

	@Override
	protected int getId() {
		return 0;
	}

	@Override
	protected int getLength() {
		return 0;
	}
}

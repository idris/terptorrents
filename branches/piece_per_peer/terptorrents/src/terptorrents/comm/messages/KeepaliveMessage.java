package terptorrents.comm.messages;

public class KeepaliveMessage extends AbstractMessage {

	@Override
	protected int getId() {
		return 0;
	}

	@Override
	protected int getLength() {
		return 0;
	}
}

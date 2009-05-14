package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;

public abstract class AbstractMessage implements Message {
	protected abstract int getId();

	public void read(DataInputStream dis, int length) throws IOException {}

	public void write(DataOutputStream out) throws IOException {
		int length = getLength();
		out.writeInt(length);
		out.writeByte(getId());
	}

	public void onReceive(PeerConnection connection) {}

	public void onSend(PeerConnection connection) {}

	protected int getLength() {
		return 1;
	}

	public String toString() {
		return getClass().getSimpleName();
	}
}

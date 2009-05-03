package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;

public abstract class Message {
	protected abstract int getId();
	public void read(DataInputStream dis, int length) throws IOException {}

	/**
	 * callback called after this message is sent to a peer
	 * @param connection
	 */
	public void onSend(PeerConnection connection) {}

	/**
	 * callback called after this message is received from a peer
	 * @param connection
	 */
	public void onReceive(PeerConnection connection) {}

	protected int getLength() {
		return 1;
	}

	public void write(DataOutputStream out) throws IOException {
		int length = getLength();
		out.writeInt(length);
		if(length > 1) {
			out.writeByte(getId());
		}
	}

	public String toString() {
		return getClass().getName();
	}
}

package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;

public class CancelMessage extends Message {
	private int index;
	private int begin;
	private int length;

	public int getIndex() {
		return index;
	}

	public int getBegin() {
		return begin;
	}

	public int getBlockLength() {
		return length;
	}

	@Override
	protected int getId() {
		return 8;
	}

	@Override
	protected int getLength() {
		return 13;
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		index = dis.readInt();
		begin = dis.readInt();
		this.length = dis.readInt();
	}

	@Override
	public void onReceive(PeerConnection conn) {
		conn.cancelPeerRequest(this);
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeInt(index);
		out.writeInt(begin);
		out.writeInt(length);
	}
}
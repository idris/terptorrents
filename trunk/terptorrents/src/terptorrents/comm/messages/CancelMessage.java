package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;

public class CancelMessage extends Message {
	private int index;
	private int begin;
	private int length;


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
		// remove this from the incoming request queue or outgoing messages queue
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeInt(index);
		out.writeInt(begin);
		out.writeInt(length);
	}
}
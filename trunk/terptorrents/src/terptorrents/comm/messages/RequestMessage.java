package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;

public class RequestMessage extends Message {
	private int index;
	private int begin;
	private int length;

	public RequestMessage() {}

	public RequestMessage(int index, int begin, int length) {
		this.index = index;
		this.begin = begin;
		this.length = length;
	}

	@Override
	public int getId() {
		return 6;
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
		conn.addPeerRequest(this);
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeInt(index);
		out.writeInt(begin);
		out.writeInt(length);
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the begin
	 */
	public int getBegin() {
		return begin;
	}
	
	public int getBlockLength() {
		return length;
	}
}
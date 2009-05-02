package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.Stats;
import terptorrents.comm.PeerConnection;
import terptorrents.models.PieceManager;

public class PieceMessage extends Message {
	private int index;
	private int begin;
	private byte[] block;

	public PieceMessage() {}

	public PieceMessage(int index, int begin, byte[] block) {
		super();
		this.index = index;
		this.begin = begin;
		this.block = block;
	}

	public int getIndex() {
		return index;
	}

	public int getBegin() {
		return begin;
	}

	public int getBlockLength() {
		return block.length;
	}

	@Override
	protected int getId() {
		return 7;
	}

	@Override
	public int getLength() {
		return 9 + block.length;
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		index = dis.readInt();
		begin = dis.readInt();

		block = new byte[length - 9];
		dis.readFully(block);
	}

	@Override
	public void onSend(PeerConnection conn) {
		Stats.getInstance().uploaded.addAndGet(block.length);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		try {
			PieceManager.getInstance().updateBlock(index, begin, block.length, block);
			Stats.getInstance().downloaded.addAndGet(block.length);
		} catch(Exception ex) {
			// something was wrong with this block
		}
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeInt(index);
		out.writeInt(begin);
		out.write(block);
	}
}
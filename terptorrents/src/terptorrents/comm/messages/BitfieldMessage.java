package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

import terptorrents.comm.PeerConnection;
import terptorrents.models.Peer;
import terptorrents.models.PieceManager;
import terptorrents.util.BitSetUtil;

public class BitfieldMessage extends Message {
	BitSet bitfield;

	@Override
	public int getId() {
		return 5;
	}

	@Override
	public int getLength() {
		return 1 + (bitfield.size() / 8);
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		byte[] bytes = new byte[--length];
		dis.readFully(bytes);
	}

	@Override
	public void onReceive(PeerConnection from) {
		PieceManager.getInstance().addPeer(bitfield, from.getPeer());
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.write(BitSetUtil.toByteArray(bitfield));
	}
}

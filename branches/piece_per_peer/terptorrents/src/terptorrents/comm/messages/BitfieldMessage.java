package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

import terptorrents.comm.PeerConnection;
import terptorrents.io.IO;
import terptorrents.models.PieceManager;
import terptorrents.util.BitSetUtil;

public class BitfieldMessage extends AbstractMessage {
	BitSet bitfield;

	public BitfieldMessage() {
		
	}
	
	public BitfieldMessage(BitSet bitfield) {
		this.bitfield = bitfield;
	}

	@Override
	public int getId() {
		return 5;
	}

	@Override
	public int getLength() {
		int bitfieldLength = IO.getInstance().getBitSet().totalNumOfPieces();
		return 1 + 
		((bitfieldLength % 8 == 0) ? (bitfieldLength / 8) : 
			(bitfieldLength/ 8 + 1));
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		byte[] bytes = new byte[--length];
		dis.readFully(bytes);
		bitfield = BitSetUtil.fromByteArray(bytes);
		
	}

	@Override
	public void onReceive(PeerConnection from) {
		PieceManager.getInstance().addPeer(bitfield, from.getPeer());
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.write(BitSetUtil.toByteArray(bitfield, getLength()-1));
	}
}

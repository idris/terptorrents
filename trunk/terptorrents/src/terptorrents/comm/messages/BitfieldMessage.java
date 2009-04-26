package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BitfieldMessage extends Message {
	byte[] bitfield;

	@Override
	public int getId() {
		return 5;
	}

	@Override
	public int getLength() {
		return 1 + bitfield.length;
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		byte[] bytes = new byte[--length];
		dis.readFully(bytes);
	}

	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.write(bitfield);
	}
}

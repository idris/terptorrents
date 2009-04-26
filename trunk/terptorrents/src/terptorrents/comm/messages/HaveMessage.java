package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HaveMessage extends Message {
	private int index;

	public int getIndex() {
		return index;
	}

	@Override
	public int getId() {
		return 4;
	}

	@Override
	public int getLength() {
		return 5;
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		index = dis.readInt();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeInt(index);
	}
}

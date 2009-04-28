package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Message {
	protected abstract int getId();
	public void read(DataInputStream dis, int length) throws IOException {}

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
}
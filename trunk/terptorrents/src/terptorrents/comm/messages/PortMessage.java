package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * not used yet
 *
 */
public class PortMessage extends Message {
	private int port;

	public PortMessage() {}

	public PortMessage(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}


	@Override
	protected int getId() {
		return 9;
	}

	@Override
	protected int getLength() {
		return 3;
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		port = dis.readChar();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeChar(port);
	}
}

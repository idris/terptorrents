package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;

public class KeepaliveMessage implements Message {
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(0);
	}

	@Override
	public void onReceive(PeerConnection connection) {}

	@Override
	public void onSend(PeerConnection connection) {}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {}
}

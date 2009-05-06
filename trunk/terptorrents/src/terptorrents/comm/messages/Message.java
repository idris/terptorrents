package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;

public interface Message {
	/**
	 * 
	 * @param dis
	 * @param length
	 * @throws IOException
	 */
	public void read(DataInputStream dis, int length) throws IOException;

	/**
	 * 
	 * @param out
	 * @throws IOException
	 */
	public void write(DataOutputStream out) throws IOException;

	/**
	 * callback called after this message is received from a peer
	 * @param connection
	 */
	public void onReceive(PeerConnection connection);

	/**
	 * callback called after this message is sent to a peer
	 * @param connection
	 */
	public void onSend(PeerConnection connection);
}

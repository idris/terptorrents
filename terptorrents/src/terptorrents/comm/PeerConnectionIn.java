package terptorrents.comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

import terptorrents.comm.messages.BitfieldMessage;
import terptorrents.comm.messages.CancelMessage;
import terptorrents.comm.messages.ChokeMessage;
import terptorrents.comm.messages.HaveMessage;
import terptorrents.comm.messages.InterestedMessage;
import terptorrents.comm.messages.KeepaliveMessage;
import terptorrents.comm.messages.Message;
import terptorrents.comm.messages.NotInterestedMessage;
import terptorrents.comm.messages.PieceMessage;
import terptorrents.comm.messages.PortMessage;
import terptorrents.comm.messages.RequestMessage;
import terptorrents.comm.messages.UnchokeMessage;
import terptorrents.exceptions.UnknownMessageException;


/**
 * 
 * @author idris
 *
 */
public class PeerConnectionIn implements Runnable {
	private final PeerConnection connection;
	private final DataInputStream in;

	public PeerConnectionIn(PeerConnection connection) throws IOException {
		this.connection = connection;
		this.in = new DataInputStream(connection.socket.getInputStream());
	}

	public void run() {
		while(!connection.disconnect) {
			try {
				readMessage();
			} catch(Exception ex) {
				
			}
		}
	}

	private Message readMessage() throws IOException, UnknownMessageException {
		int length = in.readInt();
		if(length < 0) {
			throw new UnknownMessageException("Length is negative");
		}

		connection.lastReceived = new Date();

		if(length == 0) {
			return new KeepaliveMessage();
		}

		byte id = in.readByte();

		Message m;
		switch(id) {
		case 0:
			m = new ChokeMessage();
			break;
		case 1:
			m = new UnchokeMessage();
			break;
		case 2:
			m = new InterestedMessage();
			break;
		case 3:
			m = new NotInterestedMessage();
			break;
		case 4:
			m = new HaveMessage();
			break;
		case 5:
			m = new BitfieldMessage();
			break;
		case 6:
			m = new RequestMessage();
			break;
		case 7:
			m = new PieceMessage();
			break;
		case 8:
			m = new CancelMessage();
			break;
		case 9:
			m = new PortMessage();
			break;
		default:
			throw new UnknownMessageException("Unknown Message Id: " + id);
		}

		m.read(in, length);
		m.onReceive(connection);

		return m;
	}
}

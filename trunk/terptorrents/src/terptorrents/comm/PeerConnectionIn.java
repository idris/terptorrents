package terptorrents.comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

import terptorrents.Stats;
import terptorrents.comm.messages.BitfieldMessage;
import terptorrents.comm.messages.CancelMessage;
import terptorrents.comm.messages.ChokeMessage;
import terptorrents.comm.messages.HandshakeMessage;
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
class PeerConnectionIn implements Runnable {
	private final PeerConnection connection;
	private final DataInputStream in;

	public PeerConnectionIn(PeerConnection connection) throws IOException {
		this.connection = connection;
		this.in = new DataInputStream(connection.socket.getInputStream());
	}

	public void run() {
		if(!connection.handshook) {
			try {
				// receive initial handshake
				readHandshake();
			} catch(IOException ex) {
				connection.teardown();
				return;
			}
		}

		while(!connection.disconnect) {
			try {
				readMessage();
			} catch(Exception ex) {
				
			}
		}

		connection.teardown();
	}

	private Message readHandshake() throws IOException {
		int length = in.readByte();
		HandshakeMessage handshake = new HandshakeMessage();
		handshake.read(in, length);
		return handshake;
	}

	private Message readMessage() throws IOException, UnknownMessageException {
		int one, two, three, four;
		one = in.readByte();
		two = in.readByte();
		three = in.readByte();
		four = in.readByte();
//		int length = in.readInt();
		int length = (one & 0xF000) | (two & 0x0F00) | (three & 0x00F0) | four;

		if(length < 0) {
			throw new UnknownMessageException("Length is negative");
		}

		System.out.println("=== NEW MESSAGE: " + length + " ===");
		System.out.println("first byte: " + one);

		connection.lastReceived = new Date();

		if(length == 0) {
			return new KeepaliveMessage();
		}

		byte id = in.readByte();
		System.out.println("Message ID: " + id);

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
			length -= 1;
			while(length > 0) {
				length -= in.skip(length);
			}
			throw new UnknownMessageException("Unknown Message Id: " + id);
		}

		if(m instanceof PieceMessage) {
			long start = System.currentTimeMillis();
			m.read(in, length);
			connection.downloadRate =  (((PieceMessage)m).getLength() - 1) / ((System.currentTimeMillis() - start) / 1000);
			Stats.getInstance().downloaded.addAndGet(((PieceMessage)m).getBlockLength());
			connection.lastPieceReceived = new Date();
		} else {
			m.read(in, length);
		}
		m.onReceive(connection);

		System.out.println("TYPE: " + m.getClass().getName());

		return m;
	}
}

package terptorrents.comm;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Date;

import terptorrents.Main;
import terptorrents.Stats;
import terptorrents.comm.messages.*;
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
		this.in = new DataInputStream(
				new BufferedInputStream(
						connection.socket.getInputStream()));
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
			} catch(IOException ex) {
				Main.dprint("ConnectionIN. Peer disconnected. " + connection.peer.toString());
				connection.disconnect = true;
			} catch(UnknownMessageException ex) {
				Main.dprint("Unknown message received.");
			}
		}

		connection.teardown();
	}

	private HandshakeMessage readHandshake() throws IOException {
		int length = in.read();
		if(length < 0) throw new EOFException();
		System.out.println("READING HANDSHAKE from " + connection.peer.toString());
		HandshakeMessage handshake = new HandshakeMessage();
		handshake.read(in, length);
		System.out.println("DONE READING HANDSHAKE");
		connection.handshook = true;
		return handshake;
	}

	private Message readMessage() throws IOException, UnknownMessageException {
/*
		int one, two, three, four;
		one = in.read();
		two = in.read();
		three = in.read();
		four = in.read();
		int length = (one & 0xF000) | (two & 0x0F00) | (three & 0x00F0) | four;
*/
		int length = in.readInt();

		if(length < 0) {
			if(length < 0) throw new EOFException();
		}

		//System.out.println("=== INCOMING MESSAGE: length " + length + " ===");

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
			length -= 1;
			while(length > 0) {
				length -= in.skip(length);
			}
			throw new UnknownMessageException("Unknown Message Id: " + id);
		}

		Main.dprint("=> " + m.toString() + " RECEIVED FROM " + connection.peer.toString());

		if(m instanceof PieceMessage) {
			long start = System.currentTimeMillis();
			m.read(in, length);
			connection.downloadRate =  (((PieceMessage)m).getLength() - 1) / ((double)(System.currentTimeMillis() - start) / 1000);
			Stats.getInstance().downloaded.addAndGet(((PieceMessage)m).getBlockLength());
			connection.lastPieceReceived = new Date();
		} else {
			m.read(in, length);
		}
		m.onReceive(connection);

		return m;
	}
}

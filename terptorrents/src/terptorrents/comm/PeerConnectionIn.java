package terptorrents.comm;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Date;

import terptorrents.Main;
import terptorrents.Stats;
import terptorrents.comm.messages.*;
import terptorrents.comm.messages.extended.ExtendedHandshakeMessage;
import terptorrents.comm.messages.extended.UTPEXMessage;
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
		try {
			if(!connection.handshook) {
				try {
					// receive initial handshake
					readHandshake();
				} catch(IOException ex) {
					connection.close();
				}
			}

			connection.sendBitfield();

			while(!connection.disconnect) {
				try {
					readMessage();
				} catch(IOException ex) {
					Main.dprint("ConnectionIN. Peer disconnected. " + connection.peer.toString());
					connection.close();
				} catch(UnknownMessageException ex) {
					Main.dprint("Unknown message received.");
				}
			}
		} finally {
			connection.teardown();
		}
	}

	private HandshakeMessage readHandshake() throws IOException {
		int length = in.read();
		if(length < 0) throw new EOFException();
		Main.dprint("Reading Handshake from " + connection.peer.toString());
		HandshakeMessage handshake = new HandshakeMessage();
		handshake.read(in, length);
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

		Message m = null;
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
		}

		if(Main.SUPPORT_EXTENDED_MESSAGES && id == 20 && length >= 2) {
			int extendedId = in.readByte();
			Main.iprint("INCOMING EXTENDED MESSAGE: " + extendedId);
			switch(extendedId) {
			case ExtendedHandshakeMessage.ID:
				m = new ExtendedHandshakeMessage(connection);
				break;
			case UTPEXMessage.ID:
				m = new UTPEXMessage(connection);
				break;
			}

			if(m == null){
				length -= 1; // we will skip the rest, so make sure we account for that one byte.
				Main.iprint("Unknown Extended Message ID: " + extendedId);
			}
		}

		if(m == null) {
			// unknown message. skip it.
			length -= 1;
			while(length > 0) {
				length -= in.skip(length);
			}
			Main.iprint("Unknown Message ID: " + id);
			throw new UnknownMessageException("Unknown Message Id: " + id);
		}

		Main.iprint("<= receiving " + m.getClass().getSimpleName() + " FROM " + connection.peer.toString());

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

//		Main.iprint("<= received " + m.toString() + " FROM " + connection.peer.toString());

		return m;
	}
}

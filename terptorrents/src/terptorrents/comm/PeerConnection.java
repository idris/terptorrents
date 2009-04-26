package terptorrents.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import terptorrents.comm.messages.*;
import terptorrents.exceptions.UnknownMessageException;
import terptorrents.models.Peer;

public class PeerConnection implements Runnable {
	private final Peer peer;
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;

	private Date lastReceived;
	private boolean amChoking = true;
	private boolean amInterested = false;
	private boolean peerChoking = true;
	private boolean peerInterested = false;

	private LinkedBlockingQueue<RequestMessage> incomingRequests = new LinkedBlockingQueue<RequestMessage>();
	private LinkedBlockingQueue<RequestMessage> outgoingRequests = new LinkedBlockingQueue<RequestMessage>();

	/**
	 * 
	 * @param peer
	 * @param socket - an already established (and handshaken) socket.
	 * @throws IOException
	 */
	public PeerConnection(Peer peer, Socket socket) throws IOException {
		this.peer = peer;
		this.socket = socket;
		lastReceived = new Date();

		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
	}

	public void run() {
		try {
			while(true) {
				readMessage();
			}
		} catch(Exception ex) {
			
		} finally {
			close();
		}
	}


	public boolean amChoking() {
		return amChoking;
	}

	public boolean amInterested() {
		return amInterested;
	}

	public boolean peerChoking() {
		return peerChoking;
	}

	public boolean peerInterested() {
		return peerInterested;
	}

	public boolean canIRequest() {
		return amInterested && !peerChoking;
	}

	public boolean canPeerRequest() {
		return peerInterested && !amChoking;
	}

	private void sendMessage(Message message) throws IOException {
		message.write(out);
		out.flush();
	}

	private Message readMessage() throws IOException, UnknownMessageException {
		int length = in.readInt();
		if(length < 0) {
			throw new UnknownMessageException("Length is negative");
		}

		lastReceived = new Date();

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

		return m;
	}

	private void close() {
		try {
			in.close();
			out.close();
			socket.close();
		} catch(IOException ex) {
			peer.setConnection(null);
		}
	}
}

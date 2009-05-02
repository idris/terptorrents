package terptorrents.comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import terptorrents.comm.messages.HandshakeMessage;
import terptorrents.exceptions.BadHandshakeException;
import terptorrents.exceptions.InvalidProtocolException;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;

public class PeerListener implements Runnable {
	private boolean listenForConnections = true;
	private ServerSocket serverSocket;

	public PeerListener(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
	}

	/**
	 * Listen for incoming peer connections on the specified port.
	 * When a new connection comes in, start a PeerConnection for it.
	 * @throws IOException
	 */
	public void run() {
		Socket socket = null;
		while(listenForConnections) {
			try {
				socket = serverSocket.accept();
				HandshakeMessage handshake = getHandshake(socket);
				if(handshake.getInfoHash() != infoHash) {
					throw new BadHandshakeException();
				}

				// find (or create) the peer
				Peer peer = PeerList.getInstance().getPeer(handshake.getPeerId());
				if(peer == null) {
					peer = new Peer(handshake.getPeerId(), socket.getInetAddress().getHostAddress(), socket.getPort());
				}
				if(peer.getConnection() != null) {
					// already connected to peer
				} else {
					PeerConnection connection = new PeerConnection(peer, socket);
					ConnectionPool.getInstance().addIncomingConnection(connection);
				}
			} catch(InterruptedException ex) {
				// trouble adding connection to pool
			} catch(Exception ex) {
				// something went wrong with the handshake. drop the connection
				try {
					socket.close();
				} catch(Exception ex2) {}
			}
		}

		try {
			serverSocket.close();
		} catch(Exception ex) {
			
		}
	}

	private HandshakeMessage getHandshake(Socket socket) throws InvalidProtocolException, IOException {
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		int handshakeLength = dis.readByte();
		HandshakeMessage handshake = new HandshakeMessage();
		handshake.read(dis, handshakeLength);
		return handshake;
	}

	public void stopListening() {
		listenForConnections = false;
	}
}
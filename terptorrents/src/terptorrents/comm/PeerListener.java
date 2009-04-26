package terptorrents.comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import terptorrents.comm.messages.HandshakeMessage;
import terptorrents.exceptions.InvalidProtocolException;
import terptorrents.models.Peer;

public class PeerListener {

	/**
	 * Listen for incoming peer connections on the specified port.
	 * When a new connection comes in, start a PeerConnection for it.
	 * @param port
	 * @throws IOException
	 */
	public void listen(int port) throws IOException {
		boolean listenForConnections = true;

		ServerSocket serverSocket = new ServerSocket(port);
		while(listenForConnections) {
			Socket socket = serverSocket.accept();
			try {
				HandshakeMessage handshake = getHandshake(socket);

				// make sure handshake.getInfoHash() matches

				// find the peer
				Peer peer = null;
				if(peer.getConnection() != null) {
					// already connected to peer
				} else {
					PeerConnection connection = new PeerConnection(peer, socket);
					peer.setConnection(connection);
					new Thread(connection).run();
				}
			} catch(Exception ex) {
				// something went wrong with the handshake. drop the connection
				socket.close();
			}
		}

		serverSocket.close();
	}

	private HandshakeMessage getHandshake(Socket socket) throws InvalidProtocolException, IOException {
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		int handshakeLength = dis.readByte();
		HandshakeMessage handshake = new HandshakeMessage();
		handshake.read(dis, handshakeLength);
		return handshake;
	}
}
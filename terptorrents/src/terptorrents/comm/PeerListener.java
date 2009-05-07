package terptorrents.comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import terptorrents.Main;
import terptorrents.comm.messages.HandshakeMessage;
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
		try {
			while(listenForConnections
					&& ConnectionPool.getInstance().acquireIncomingSlot()) {

				try {
					socket = serverSocket.accept();

					HandshakeMessage handshake = getHandshake(socket);

					ConnectionPool.getInstance().connectLock.lock();

					try {
						// find the peer
						Peer peer = PeerList.getInstance().getPeer(
								new InetSocketAddress(
										socket.getInetAddress().getHostAddress(), 
										socket.getPort()));

						if(peer == null) {
							// prepare new peer
							peer = new Peer(handshake.getPeerId(), socket.getInetAddress().getHostAddress(), socket.getPort());
							PeerList.getInstance().addPeer(peer);
						}

						if(!peer.isConnected()) {
							/* Sergey (Idris)
							 * open connection to the peer only if it is not 
							 * already connected. This will avoid existence
							 * of multiple connections
							 */
							PeerConnection connection = new PeerConnection(peer, socket);
							ConnectionPool.getInstance().addIncomingConnection(connection);
						}
					} finally {
						ConnectionPool.getInstance().connectLock.unlock();
					}

				} catch(IOException ex) {
					// something went wrong with the handshake. drop the connection
					ConnectionPool.getInstance().releaseIncomingSlot();
					try {
						socket.close();
					} catch(Exception ex2) {}
				}
			}
		} catch(InterruptedException ex) {
			Main.dprint("PeerListener. Interrupted exception is caught. Not Supported. Ignoring");
		}

		try {
			serverSocket.close();
		} catch(Exception ex) {

		}
	}

	private HandshakeMessage getHandshake(Socket socket) throws IOException {
		System.out.println("INCOMING HANDSHAKE FROM " + socket.getInetAddress().toString());
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		int handshakeLength = dis.read();
		HandshakeMessage handshake = new HandshakeMessage();
		handshake.read(dis, handshakeLength);
		System.out.println("DONE WITH INCOMING HANDSHAKE");
		return handshake;
	}

	public void stopListening() {
		listenForConnections = false;
	}
}
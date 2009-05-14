package terptorrents.comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import terptorrents.Main;
import terptorrents.comm.messages.HandshakeMessage;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;

public class PeerListener implements Runnable {
	private boolean listenForConnections = true;
	private ServerSocket serverSocket;

	public PeerListener(ServerSocket ss) throws IOException {
		this.serverSocket = ss;
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
										socket.getInetAddress()
										);

						if(new String(handshake.getPeerId()).equals(new String(Main.PEER_ID))) {
							if(peer != null) PeerList.getInstance().removePeer(peer);
							throw new IOException("Tried to connect to myself!");
						}

						if(peer == null) {
							// prepare new peer
							peer = new Peer(handshake.getPeerId(), socket.getInetAddress().getHostAddress());
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
						} else {
							Main.dprint("Peer " + peer.toString() + " is already connected.");
						}
					} finally {
						ConnectionPool.getInstance().connectLock.unlock();
					}

				} catch(IOException ex) {
					Main.dprint("Problem with Incoming Handshake: " + ex.getMessage());
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
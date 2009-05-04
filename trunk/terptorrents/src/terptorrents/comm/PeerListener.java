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

	public PeerListener(int port){
		int i;
		for(i = 0; i < 10; i++){
			try {
				this.serverSocket = new ServerSocket(port + i);
				break;
			} catch (IOException e) {
				if(Main.DEBUG){
					System.out.println("trying new port to listen:" + port +"\n");
				}
			}
		}
		if(i == 10){
			throw new InternalError("No available ports\n");
		}
	}

	/**
	 * Listen for incoming peer connections on the specified port.
	 * When a new connection comes in, start a PeerConnection for it.
	 * @throws IOException
	 */
	public void run() {
		Socket socket = null;
		try {
			while(listenForConnections && ConnectionPool.getInstance().acquireIncomingSlot()) {
				try {
					socket = serverSocket.accept();

					//TODO check the number of active connections, drop sockets if it is over limit

					HandshakeMessage handshake = getHandshake(socket);

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
				} catch(IOException ex) {
					// something went wrong with the handshake. drop the connection
					ConnectionPool.getInstance().releaseIncomingSlot();
					try {
						socket.close();
					} catch(Exception ex2) {}
				}
			}
		} catch(InterruptedException ex) {
			//
			ex.printStackTrace();
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
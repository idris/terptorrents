package terptorrents.comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import metainfo.TorrentParser;

import terptorrents.Main;
import terptorrents.comm.messages.HandshakeMessage;
import terptorrents.exceptions.BadHandshakeException;
import terptorrents.exceptions.InvalidProtocolException;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;

public class PeerListener implements Runnable {
	private boolean listenForConnections = true;
	private ServerSocket serverSocket;

	public PeerListener(int port){
		int i;
		for(i = port; i < 10; i++){
			try {
				this.serverSocket = new ServerSocket(i);
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
		while(listenForConnections) {
			try {
				socket = serverSocket.accept();

				//TODO check the number of active connections, drop sockets if it is over limit

				HandshakeMessage handshake = getHandshake(socket);

				if (!Arrays.equals(handshake.getInfoHash(), 
						TorrentParser.getInstance().getMetaFile().getByteInfoHash())) {
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
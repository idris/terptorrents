package terptorrents.comm;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import terptorrents.Main;
import terptorrents.Stats;
import terptorrents.comm.messages.*;

/**
 * 
 * @author idris
 *
 */
class PeerConnectionOut implements Runnable {
	private final PeerConnection connection;
	private final DataOutputStream out;

	public PeerConnectionOut(PeerConnection connection) throws IOException {
		this.connection = connection;
		this.out = new DataOutputStream(
				new BufferedOutputStream(
				connection.socket.getOutputStream()));
	}

	public void run() {
		while(!connection.disconnect) {
			if(connection.peerIsDead()) {
				connection.close();
				break;
			}
			try {
				Message toSend = connection.outgoingMessages.poll(2, TimeUnit.MINUTES);
				if(toSend == null) {
					toSend = new KeepaliveMessage();
				}
				writeMessage(toSend);
			} catch(InterruptedException ex) {
				// keep on truckin'
			} catch(IOException ex) {
				Main.dprint("ConnectionOUT. Peer disconnected. " + connection.peer.toString());
				connection.close();
			}
		}

		connection.teardown();
	}

	private void writeMessage(Message message) throws IOException {
		long start = System.currentTimeMillis();
		message.write(out);
		out.flush();
		if(message instanceof PieceMessage) {
			connection.bytesUploaded += ((PieceMessage)message).getLength();
			connection.timeUploaded += (double)(System.currentTimeMillis() - start) / 1000;
			Stats.getInstance().uploaded.addAndGet(((PieceMessage)message).getBlockLength());
		}
		Main.iprint("=> " + message.toString() + " SENT to " + connection.peer.toString());

		//System.out.println("=== SENT MESSAGE to " + connection.peer.getAddress().toString() + " ===\n" + message.toString());

		message.onSend(connection);
	}
}

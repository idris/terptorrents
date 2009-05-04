package terptorrents.comm;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import terptorrents.Stats;
import terptorrents.comm.messages.KeepaliveMessage;
import terptorrents.comm.messages.Message;
import terptorrents.comm.messages.PieceMessage;

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
				connection.disconnect = true;
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
				ex.printStackTrace();
			} catch(IOException ex) {
				// oh noes!
				ex.printStackTrace();
				connection.disconnect = true;
			}
		}

		connection.teardown();
	}

	private void writeMessage(Message message) throws IOException {
		long start = System.currentTimeMillis();
		message.write(out);
		out.flush();
		if(message instanceof PieceMessage) {
			connection.uploadRate =  ((PieceMessage)message).getLength() / ((System.currentTimeMillis() - start) / 1000);
			Stats.getInstance().uploaded.addAndGet(((PieceMessage)message).getBlockLength());
		}

		System.out.println("=== SENT MESSAGE to " + connection.peer.getAddress().toString() + " ===\n" + message.toString());

		message.onSend(connection);
	}
}

package terptorrents.comm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import terptorrents.comm.messages.Message;

/**
 * 
 * @author idris
 *
 */
public class PeerConnectionOut implements Runnable {
	private final PeerConnection connection;
	private final DataOutputStream out;

	public PeerConnectionOut(PeerConnection connection) throws IOException {
		this.connection = connection;
		this.out = new DataOutputStream(connection.socket.getOutputStream());
	}

	public void run() {
		while(!connection.disconnect) {
			if(connection.peerIsDead()) {
				connection.disconnect = true;
				break;
			}
			try {
				writeMessage(connection.outgoingMessages.poll(10, TimeUnit.SECONDS));
			} catch(InterruptedException ex) {
				// keep on truckin'
			} catch(IOException ex) {
				// oh noes!
			}
		}

		connection.close();
	}

	private void writeMessage(Message message) throws IOException {
		message.write(out);
		out.flush();
		message.onSend(connection);
	}
}
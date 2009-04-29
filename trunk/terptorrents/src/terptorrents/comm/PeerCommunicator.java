package terptorrents.comm;

import java.net.Socket;
import java.util.Map;
import java.util.Queue;

import terptorrents.models.Peer;

import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

public class PeerCommunicator {
	Map<Peer, Queue<Message>> outgoingMessages;

	public void sendMessage(Message message, Peer peer) {
		outgoingMessages.get(peer).add(message);
	}

	
}

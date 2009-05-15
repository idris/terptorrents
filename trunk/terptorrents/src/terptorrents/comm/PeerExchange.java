package terptorrents.comm;

import java.util.Set;

import terptorrents.comm.messages.extended.UTPEXMessage;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;

public class PeerExchange implements Runnable {
	public static final int PEER_EXCHANGE_INTERVAL = 60 * 1000;
	private boolean go = true;

	public void run() {
		while(go) {
			try {
				Thread.sleep(PEER_EXCHANGE_INTERVAL);
				Set<Peer> peers = PeerList.getInstance().getWellKnownPeers();
				for(PeerConnection conn: ConnectionPool.getInstance().getConnections()) {
					UTPEXMessage m = new UTPEXMessage(conn);
					m.setPeers(peers);
					conn.sendMessage(m);
				}
			} catch(Exception ex) {
				
			}
		}
	}

	public void stop() {
		this.go = false;
	}
}

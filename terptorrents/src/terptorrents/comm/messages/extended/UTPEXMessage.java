package terptorrents.comm.messages.extended;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.istack.internal.NotNull;

import metainfo.BEValue;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.TrackerCommunicator;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;

/**
 * http://trac.transmissionbt.com/browser/trunk/doc/extended-messaging.txt?rev=2919
 * 
 * @author idris
 */
public class UTPEXMessage extends BEncodedMessage {
	public static final int ID = 1;
	private static final int MAX_PEERS_TO_EXCHANGE = 50;
	private Set<Peer> peers = new HashSet<Peer>();

	public UTPEXMessage(PeerConnection conn) {
		super(conn);
	}

	public void setPeers(Set<Peer> peers) {
		this.peers = peers;
	}

	@Override
	protected String getId() {
		return "ut_pex";
	}

	@Override
	public Map<String, ?> getMap() {
		Map<String, Object> map = new HashMap<String, Object>();

		

		map.put("added", toBytes(peers));
//		map.put("added.f", flagBytes(peers));
//		map.put("dropped", "same as added, but these are newly dead peers");
		return map;
	}

	@Override
	public void readBEMap(Map map) throws IOException {
		PeerList peerList = PeerList.getInstance();
		Main.dprint("Got Peer Exchange Message (ut_pex) from " + conn.toString() + ". Added:");

		BEValue addedBE = ((BEValue)map.get("added"));
		if(addedBE != null) {
			List<Peer> added = TrackerCommunicator.readPeers(addedBE.getBytes());
			List<Byte> flags = new ArrayList<Byte>(added.size());

			if(false) { // we don't need these flags
				BEValue addedFBE = ((BEValue)map.get("added.f"));
				if(addedFBE != null) {
					for(byte f: addedFBE.getBytes()) {
						flags.add(f);
					}
				}
			}

			peerList.addPeers(added);
		}

		if(true) return; // we'll take care of dropping ourselves. thanks anyway

		BEValue droppedBE = ((BEValue)map.get("dropped"));
		if(droppedBE != null) {
			List<Peer> dropped = TrackerCommunicator.readPeers(droppedBE.getBytes());
			for(Peer d: dropped) {
				Peer p = peerList.getPeer(d.getAddress());
				if(!p.isConnected()) PeerList.getInstance().removePeer(d);
			}
		}
	}

	private static byte[] toBytes(Set<Peer> peers) {
		byte[] bytes = new byte[Math.max(peers.size(), MAX_PEERS_TO_EXCHANGE) * 6];
		int i = 0;
		for(Peer p: peers) {
			if(i >= MAX_PEERS_TO_EXCHANGE) break;
			int b = i * 6;
			byte[] addr = p.getAddress().getAddress();
			bytes[b] = addr[0];
			bytes[b+1] = addr[1];
			bytes[b+2] = addr[2];
			bytes[b+3] = addr[3];
			bytes[b+4] = (byte)((p.getPort() & 0xFF00) >> 8);
			bytes[b+5] = (byte)(p.getPort() & 0xFF);
			i++;
		}

		return bytes;
	}

	private static byte[] flagBytes(Set<Peer> peers) {
		byte[] bytes = new byte[Math.max(peers.size(), MAX_PEERS_TO_EXCHANGE) * 1];
		return bytes;
	}
}

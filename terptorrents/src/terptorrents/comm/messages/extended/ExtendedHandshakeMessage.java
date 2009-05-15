package terptorrents.comm.messages.extended;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;

import metainfo.BDecoder;
import metainfo.BEValue;

/**
 * http://www.rasterbar.com/products/libtorrent/extension_protocol.html
 * 
 * @author idris
 */
public class ExtendedHandshakeMessage extends BEncodedMessage {
	public static final int ID = 0;
	Map<String, Object> map;

	public ExtendedHandshakeMessage(PeerConnection conn) {
		super(conn);
	}

	@Override
	protected String getId() {
		return "handshake";
	}

	@Override
	public Map<String, Object> getMap() {
		Map<String, Object> supportedMessages = new HashMap<String, Object>();
		supportedMessages.put("ut_pex", UTPEXMessage.ID);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("m", supportedMessages);
		map.put("p", Main.PORT);
		map.put("v", "TerpTorrents v1.0");

		return map;
	}

	@Override
	public void readBEMap(Map map) throws IOException {
		BEValue mBE = ((BEValue)map.get("m"));
		if(mBE != null) {
			Map m = mBE.getMap();
			BEValue pexBE = (BEValue)m.get("ut_pex");
			if(pexBE != null) {
				int pexId = pexBE.getInt();
				conn.addExtendedMessageType("ut_pex", pexId);
			}
		}

		BEValue pBE = ((BEValue)map.get("p"));
		if(pBE != null) {
			int listenPort = pBE.getInt();
			conn.getPeer().addPort(listenPort);
		}

		BEValue vBE = ((BEValue)map.get("v"));
		if(vBE != null) {
			String client = vBE.getString();
		}

		BEValue youripBE = ((BEValue)map.get("yourip"));
		if(youripBE != null) {
//			InetAddress address = youripBE.getBytes();
		}

		BEValue ipv6BE = (BEValue)map.get("ipv6");
		if(ipv6BE != null) {
//			Inet6Address ipv6 = ipv6BE.getBytes();
		}

		BEValue ipv4BE = (BEValue)map.get("ipv4");
		if(ipv4BE != null) {
//			Inet6Address ipv4 = ipv4BE.getBytes();
		}

		BEValue reqqBE = (BEValue)map.get("reqq");
		if(reqqBE != null) {
			int maxOutstandingRequests = reqqBE.getInt();
		}
	}
}

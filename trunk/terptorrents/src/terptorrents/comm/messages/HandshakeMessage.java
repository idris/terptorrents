package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import metainfo.TorrentParser;

import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.Message;
import terptorrents.exceptions.BadHandshakeException;

public class HandshakeMessage implements Message {
	public static final String pstr = "BitTorrent protocol";
	public static final int HANDSHAKE_MESSAGE_LEN_EXCLUDING_PSTRLEN = 49;
	private byte[] infoHash;
	private byte[] peerId;

	public HandshakeMessage() {}

	public HandshakeMessage(byte[] infoHash, byte[] peerId) {
		this.infoHash = infoHash;
		this.peerId = peerId;
	}

	public byte[] getInfoHash() {
		return infoHash;
	}

	public byte[] getPeerId() {
		return peerId;
	}


	public void read(DataInputStream dis, int pstrlen) throws IOException {
		if(pstrlen != HandshakeMessage.pstr.length()) throw new BadHandshakeException();

		byte[] pstr = new byte[pstrlen];
		dis.readFully(pstr);
		if(!HandshakeMessage.pstr.equals(new String(pstr))) throw new BadHandshakeException();

		dis.readLong(); // reserved
		byte[] twentyBytes = new byte[20];

		dis.readFully(twentyBytes); // info_hash

		infoHash = twentyBytes;

		if(!Arrays.equals(infoHash, TorrentParser.getInstance().getMetaFile().getByteInfoHash())) {
			throw new BadHandshakeException();
		}

		twentyBytes = new byte[20];

		dis.readFully(twentyBytes); // peer_id
		peerId = twentyBytes;
	}


	public void write(DataOutputStream out) throws IOException {
		out.writeByte(pstr.length()); // pstrlen
		out.writeBytes(pstr); // pstr
		out.writeLong(0); // reserved
		out.write(infoHash); // info_hash
		out.write(peerId); // peer_id
	}


	public void onReceive(PeerConnection conn) {
		conn.getPeer().setId(peerId);
	}
	public void onSend(PeerConnection conn) {}

	@Override
	public String toString() {
		return "Handshake Message";
	}
}

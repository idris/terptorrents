package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.exceptions.InvalidProtocolException;

public class HandshakeMessage extends Message {
	private static final String pstr = "BitTorrent protocol";
	private static final int HANDSHAKE_MESSAGE_LEN_EXCLUDING_PSTRLEN = 49;
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

	@Override
	protected int getId() {
		return 0;
	}

	@Override
	protected int getLength() {
		return HANDSHAKE_MESSAGE_LEN_EXCLUDING_PSTRLEN + pstr.length();
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		int pstrlen = length - 49;
		if(pstrlen != HandshakeMessage.pstr.length()) throw new InvalidProtocolException();

		
		
		byte[] pstr = new byte[pstrlen];
		dis.readFully(pstr);
		if(!HandshakeMessage.pstr.getBytes().equals(pstr)) throw new InvalidProtocolException();

		dis.readLong(); // reserved
		byte[] twentyBytes = new byte[20];
		
		dis.readFully(twentyBytes); // info_hash
		
		infoHash = twentyBytes;
		
		dis.readFully(twentyBytes); // peer_id
		peerId = twentyBytes;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeByte(pstr.length()); // pstrlen
		out.writeBytes(pstr); // pstr
		out.writeLong(0); // reserved
		out.write(infoHash); // info_hash
		out.write(peerId); // peer_id
	}
}

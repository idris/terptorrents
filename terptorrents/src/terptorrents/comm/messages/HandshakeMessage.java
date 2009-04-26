package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.exceptions.InvalidProtocolException;

public class HandshakeMessage extends Message {
	private static final String pstr = "BitTorrent protocol";
	private String infoHash;
	private String peerId;


	public String getInfoHash() {
		return infoHash;
	}

	public String getPeerId() {
		return peerId;
	}


	@Override
	protected int getId() {
		return 0;
	}

	@Override
	protected int getLength() {
		return 49 + pstr.length();
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
		infoHash = new String(twentyBytes);
		dis.readFully(twentyBytes); // peer_id
		peerId = new String(twentyBytes);
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeByte(pstr.length()); // pstrlen
		out.writeBytes(pstr); // pstr
		out.writeLong(0); // reserved
		out.writeBytes(infoHash); // info_hash
		out.writeBytes(peerId); // peer_id
	}
}

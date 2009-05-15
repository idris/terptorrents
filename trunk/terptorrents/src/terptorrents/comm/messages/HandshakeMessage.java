package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import metainfo.TorrentParser;

import terptorrents.Main;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.Message;
import terptorrents.exceptions.BadHandshakeException;

public class HandshakeMessage implements Message {
	public static final String pstr = "BitTorrent protocol";
	public static final int HANDSHAKE_MESSAGE_LEN_EXCLUDING_PSTRLEN = 49;
	private byte[] infoHash;
	private byte[] peerId;
	private boolean allowExtendedMessages = Main.SUPPORT_EXTENDED_MESSAGES;

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

	public void allowExtendedMessages() {
		if(Main.SUPPORT_EXTENDED_MESSAGES) {
			this.allowExtendedMessages = true;
		}
	}


	public void read(DataInputStream dis, int pstrlen) throws IOException {
		if(pstrlen != HandshakeMessage.pstr.length()) throw new BadHandshakeException("Invalid Protocol Length (" + pstrlen + ")");

		byte[] pstr = new byte[pstrlen];
		dis.readFully(pstr);
		if(!HandshakeMessage.pstr.equals(new String(pstr))) throw new BadHandshakeException("Invalid Protocol: " + pstr);

		long reserved = dis.readLong(); // reserved. see http://www.bittorrent.org/beps/bep_0004.html
		if((reserved & 0x0000000000100000L) > 0) {
			// supports LibTorrent extension protocol for BitTorrent
			// http://www.rasterbar.com/products/libtorrent/extension_protocol.html
			allowExtendedMessages();
		}

		if((reserved & 0x8000000000000000L) > 0) {
			// supports Azureus Messaging Protocol
		}
		if((reserved & 0x0000000000000100L) > 0) {
			// supports BitTorrent DHT
		}

		byte[] twentyBytes = new byte[20];

		dis.readFully(twentyBytes); // info_hash

		infoHash = twentyBytes;

		if(!Arrays.equals(infoHash, TorrentParser.getInstance().getMetaFile().getByteInfoHash())) {
			throw new BadHandshakeException("Wrong InfoHash");
		}

		twentyBytes = new byte[20];

		dis.readFully(twentyBytes); // peer_id
		peerId = twentyBytes;
	}


	public void write(DataOutputStream out) throws IOException {
		out.writeByte(pstr.length()); // pstrlen
		out.writeBytes(pstr); // pstr

		long reserved = 0;
		if(allowExtendedMessages) {
			reserved &= 0x0000000000100000L;
		}
		out.writeLong(reserved); // reserved

		out.write(infoHash); // info_hash
		out.write(peerId); // peer_id
	}


	public void onReceive(PeerConnection conn) {
		conn.supportsExtendedMessages = allowExtendedMessages;
	}
	public void onSend(PeerConnection conn) {}

	@Override
	public String toString() {
		return "Handshake Message";
	}
}

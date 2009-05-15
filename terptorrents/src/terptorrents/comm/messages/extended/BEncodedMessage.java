package terptorrents.comm.messages.extended;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import metainfo.BDecoder;
import metainfo.BEValue;
import metainfo.BEncoder;

import terptorrents.comm.PeerConnection;

public abstract class BEncodedMessage extends ExtendedMessage {
	public abstract Map<String, ?> getMap();

	public BEncodedMessage(PeerConnection conn) {
		super(conn);
	}

	public abstract void readBEMap(Map map) throws IOException;

	@Override
	public void read(DataInputStream in, int length) throws IOException {
		length -= 2;
		byte[] bytes = new byte[length];
		in.readFully(bytes);
		readBEMap(BDecoder.bdecode(new ByteArrayInputStream(bytes)).getMap());
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		if(getTheirMessageId() == null) return;
		byte[] bytes = BEncoder.bencode(getMap());
		out.writeInt(bytes.length + 2);
		out.writeByte(ExtendedMessage.BITTORRENT_MESSAGE_ID);
		out.writeByte(getTheirMessageId());
		out.write(bytes);
	}
}

package terptorrents.comm.messages.extended;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.Message;

/**
 * http://www.rasterbar.com/products/libtorrent/extension_protocol.html
 * 
 * @author idris
 */
public abstract class ExtendedMessage implements Message {
	public static final int BITTORRENT_MESSAGE_ID = 20;
	protected PeerConnection conn;

	public ExtendedMessage(PeerConnection conn) {
		this.conn = conn;
	}

//	protected abstract int getLength();

	/**
	 * Remember, this is the extended message ID, not
	 * @return extended message ID
	 */
	protected abstract String getId();

	public Integer getTheirMessageId() {
		return conn.getExtendedMessageId(getId());
	}

	@Override
	public void onReceive(PeerConnection conn) {}

	@Override
	public void onSend(PeerConnection connection) {}
}

package terptorrents.comm.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import terptorrents.Main;
import terptorrents.Stats;
import terptorrents.comm.ConnectionPool;
import terptorrents.comm.PeerConnection;
import terptorrents.models.BlockRange;
import terptorrents.models.PieceManager;
import terptorrents.models.RequestManager;

public class PieceMessage extends AbstractMessage {
	private int index;
	private int begin;
	private byte[] block;

	public PieceMessage() {}

	public PieceMessage(int index, int begin, byte[] block) {
		super();
		this.index = index;
		this.begin = begin;
		this.block = block;
	}

	public int getIndex() {
		return index;
	}

	public int getBegin() {
		return begin;
	}

	public int getBlockLength() {
		return block.length;
	}

	public int getEnd() {
		return begin + block.length;
	}

	@Override
	protected int getId() {
		return 7;
	}

	@Override
	public int getLength() {
		return 9 + block.length;
	}

	@Override
	public void read(DataInputStream dis, int length) throws IOException {
		index = dis.readInt();
		begin = dis.readInt();

		block = new byte[length - 9];
		dis.readFully(block);
	}

	@Override
	public void onSend(PeerConnection conn) {
		Stats.getInstance().uploaded.addAndGet(block.length);
	}

	@Override
	public void onReceive(PeerConnection conn) {
		int toRequest = Main.MAX_OUTSTANDING_REQUESTS - conn.outstandingRequests.decrementAndGet();
		try {
			PieceManager.getInstance().updateBlock(index, begin, block.length, block, conn.getPeer());
			Stats.getInstance().downloaded.addAndGet(block.length);
		} catch(Exception ex) {
			Main.dprint("Exception in PieceMessage.onReceive() is caught: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
			//ex.printStackTrace();
		}

		// request more from this connection
		try {
			if(PieceManager.getInstance().isEndGameTiggered()) {
				 for(PeerConnection peerCon: ConnectionPool.
	                    getInstance().getNonChokingAndInsterested()) {
					 if(conn != peerCon){
						 peerCon.sendMessage(
								 new CancelMessage(index, begin, block.length));
					 }
				 }
			} else if(conn.canIRequest()) {
				if(toRequest >= Main.MAX_OUTSTANDING_REQUESTS/2) {
//					RequestManager.getInstance().requestBlocks(conn.getPeer(), toRequest); // the old way
					List<BlockRange> ranges = PieceManager.getInstance().getBlockRangeToRequestSamePiecePerPeer(conn.getPeer(), toRequest);
					for(BlockRange range: ranges) {
						conn.sendMessage(new RequestMessage(range));
					}
				}
			}
		} catch(Exception ex) {
			Main.iprint("Exception in PieceMessage{" + index + "}.onReceive() (bottom) is caught: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
		}
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeInt(index);
		out.writeInt(begin);
		out.write(block);
	}
}
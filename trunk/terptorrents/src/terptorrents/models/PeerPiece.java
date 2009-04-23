package terptorrents.models;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.util.HashSet;

public class PeerPiece extends Piece {
	private HashSet<Peer> peerSet;
	private BitSet bitMap;
	private ByteArrayOutputStream data;
	
	public PeerPiece(boolean isLastPiece){
		super(isLastPiece);
		peerSet = new HashSet<Peer>();
		data = new ByteArrayOutputStream(getSize());
		this.bitMap = new BitSet(getSize());
	}
	
	public void addPeer(Peer newPeer){
		peerSet.add(newPeer);
	}

	public void removePeer(Peer newPeer){
		peerSet.remove(newPeer);
	}
	
	public boolean Have_Piece(){
		return (bitMap.nextClearBit(0) == -1) ? true : false;
	}

	public boolean receiveBlock(int begin, int length, byte [] data){
		if(begin < 0 || begin + length > getSize())
			return false;
		bitMap.set(begin, begin+length);
		this.data.write(data, begin, length);
		return true;
	}

	public HashSet<Peer> getPeer(){
		return peerSet;
	}
}

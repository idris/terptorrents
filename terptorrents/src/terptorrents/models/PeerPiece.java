package terptorrents.models;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.util.HashSet;

import terptorrents.exceptions.BlockIndexOutOfBound;
import terptorrents.exceptions.PieceNotReadable;
import terptorrents.io.IO;

public class PeerPiece extends Piece {
	private HashSet<Peer> peerSet;
	private BitSet blockBitSet;
	private ByteArrayOutputStream data;
	
	public PeerPiece(boolean isLastPiece){
		super(isLastPiece);
		peerSet = new HashSet<Peer>();
		data = new ByteArrayOutputStream(getSize());
		this.blockBitSet = new BitSet(getSize());
	}
	
	public void addPeer(Peer newPeer){
		peerSet.add(newPeer);
	}

	public void removePeer(Peer peer){
		peerSet.remove(peer);
	}

	public HashSet<Peer> getPeerSet(){
		return peerSet;
	}

	@Override
	public byte [] requestBlock(IO io, int pieceIndex, int blockBegin, 
			int blockLength) throws PieceNotReadable{
		throw new PieceNotReadable();
	}

	@Override
	public boolean updateBlock(IO io, int pieceIndex,
			int blockBegin, int blockLength, byte [] data) 
	throws BlockIndexOutOfBound {
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new BlockIndexOutOfBound();
		blockBitSet.set(blockBegin, blockBegin + blockLength);
		this.data.write(data, blockBegin, blockLength);
		if(Have_Piece()){
			if(io.writePiece(pieceIndex, this.data.toByteArray()))
				return true;
			else{
				blockBitSet.clear();
				return false;
			}
		}else
			return false;
	}	
	
	private boolean Have_Piece(){
		return (blockBitSet.nextClearBit(0) == -1) ? true : false;
	}
}

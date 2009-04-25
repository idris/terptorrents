package terptorrents.models;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.util.HashSet;

import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotReadable;
import terptorrents.io.IO;

public class PeerPiece extends Piece {
	private HashSet<Peer> peerSet;
	private BitSet blockBitFeild;
	private ByteArrayOutputStream data;
	
	public PeerPiece(boolean isLastPiece, int index){
		super(isLastPiece, index);
		peerSet = new HashSet<Peer>();
		data = new ByteArrayOutputStream(getSize());
		this.blockBitFeild = new BitSet(getSize());
	}

	public int getNumPeer(){
		return peerSet.size();
	}
	
	/**
	 * This function returns the the biggest undownloaded block range.
	 * 
	 * TODO this function is currently doing a linear search. This call can be
	 * very expensive.
	 * 
	 * @return
	 */
	public BlockRange getBlockRangeToRequest(){
		int max_begin = 0;
		int pieceLength = blockBitFeild.length();
		int max_length = 0;
		
		if(getNumPeer() == 0)
			return null;
		
		for(int i = 0; i < pieceLength; i++){
			if(!blockBitFeild.get(i)){
				int currentLength = blockBitFeild.nextSetBit(i) - i;
				if(currentLength >= terptorrents.Main.MAX_REQUEST_BLOCK_SIZE){
					max_length = terptorrents.Main.MAX_REQUEST_BLOCK_SIZE;
					max_begin = i;
					break;
				}
				if(currentLength > max_length){
					max_length = currentLength;
					max_begin = i;
				}
				i = currentLength + i - 1;
			}
		}
		return new BlockRange(max_begin, max_length, getIndex());
	}
	
	public void addPeer(Peer newPeer){
		peerSet.add(newPeer);
	}

	public void removePeer(Peer peer){
		if(peerSet.contains(peer))
			peerSet.remove(peer);
	}

	public HashSet<Peer> getPeerSet(){
		return peerSet;
	}

	@Override
	public byte [] requestBlock(IO io, int pieceIndex, int blockBegin, 
			int blockLength) throws TerptorrentsModelsPieceNotReadable{
		throw new TerptorrentsModelsPieceNotReadable();
	}

	@Override
	public boolean updateBlock(IO io, int pieceIndex,
			int blockBegin, int blockLength, byte [] data) 
	throws TerptorrentsModelsBlockIndexOutOfBound {
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new TerptorrentsModelsBlockIndexOutOfBound();
		blockBitFeild.set(blockBegin, blockBegin + blockLength);
		this.data.write(data, blockBegin, blockLength);
		if(Have_Piece()){
			if(io.writePiece(pieceIndex, this.data.toByteArray()))
				return true;
			else{
				blockBitFeild.clear();
				return false;
			}
		}else
			return false;
	}	
	
	private boolean Have_Piece(){
		return (blockBitFeild.nextClearBit(0) == -1) ? true : false;
	}
}

/**
 * 
 */
package terptorrents.models;

import java.util.BitSet;
import java.util.HashSet;

import terptorrents.exceptions.BlockIndexOutOfBound;
import terptorrents.exceptions.PieceIndexOutOfBound;
import terptorrents.exceptions.PieceNotReadable;
import terptorrents.exceptions.PieceNotWritable;
import terptorrents.io.IOBitSet;
import terptorrents.io.IO;

/**
 * @author jonli
 *
 */
public class PieceManager {
	private Piece [] pieces;
	private IO io;

	public PieceManager(IO io){
		this.io = io;
		IOBitSet bitMap = io.getBitSet();
		int numPieces = bitMap.totalNumOfPieces();
		
		Piece.setLastPieceSize(io.getPieceSize());
		Piece.setSize(io.getLastPieceSize());
		
		pieces = new Piece[numPieces];
		for(int i = 0; i < numPieces; i++){
			pieces[i] = (bitMap.havePiece(i)) ? new LocalPiece((i == numPieces - 1)) : 
				new PeerPiece(i == numPieces - 1);
		}
	}

	public BlockRange getBlockRangeToRequest(int pieceIndex)	
	throws PieceNotWritable, PieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getBlockRangeToRequest();
		else
			throw new PieceNotWritable();
	}
	
	public BlockRange [] getAllBlockRangeToRequest() {
		int numPieces = pieces.length;
		BlockRange [] res = new BlockRange[numPieces];
		for(int i = 0; i < numPieces; i++){
			try {
				res[i] = (pieces[i] instanceof PeerPiece) ? 
						getBlockRangeToRequest(i) : null;
			} catch (Exception e) {
				res[i] = null;
			}
		}
		return res;
	}
	
	public void addPeer(BitSet peerBitField, Peer peer) {
		int numPieces = pieces.length;
		assert peerBitField.length() == numPieces;
		for(int i = 0; i < numPieces; i++){
			if(pieces[i] instanceof PeerPiece && peerBitField.get(i))
				((PeerPiece)(pieces[i])).addPeer(peer);	
		}
	}
	
	public void removePeer(Peer peer){
		int numPieces = pieces.length;
		for(int i = 0; i < numPieces; i++)
			if(pieces[i] instanceof PeerPiece)
				((PeerPiece)(pieces[i])).removePeer(peer);			
	}
	
	public HashSet<Peer> GetPeerSet(int pieceIndex) throws PieceNotWritable, PieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getPeerSet();
		else
			throw new PieceNotWritable();
	}
	
	public byte [] requestBlock(int pieceIndex, int blockBegin, int blockLength)	
	throws PieceNotReadable, BlockIndexOutOfBound, PieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutOfBound();
		return pieces[pieceIndex].requestBlock(io, pieceIndex, blockBegin, blockLength);
	}
	
	public void updateBlock(int pieceIndex,	int blockBegin, int blockLength, byte [] data)
	throws PieceNotWritable, BlockIndexOutOfBound, PieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutOfBound();
		if(pieces[pieceIndex].updateBlock(io, pieceIndex, blockBegin, blockLength, data))
			pieces[pieceIndex] = new LocalPiece((pieceIndex == pieces.length - 1));
	}
}
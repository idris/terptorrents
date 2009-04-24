/**
 * 
 */
package terptorrents.models;

import java.util.HashSet;

import terptorrents.exceptions.BlockIndexOutOfBound;
import terptorrents.exceptions.PieceIndexOutofBound;
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

	public void addPeer(int pieceIndex, Peer peer) 
	throws PieceNotWritable, PieceIndexOutofBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutofBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			((PeerPiece)(pieces[pieceIndex])).addPeer(peer);
		else
			throw new PieceNotWritable();
	}
	
	public void removePeer(int pieceIndex, Peer peer)
	throws PieceNotWritable, PieceIndexOutofBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutofBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			((PeerPiece)(pieces[pieceIndex])).removePeer(peer);
		else
			throw new PieceNotWritable();
	}
	
	public HashSet<Peer> GetPeerList(int pieceIndex) throws PieceNotWritable, PieceIndexOutofBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutofBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getPeerSet();
		else
			throw new PieceNotWritable();
	}
	
	public byte [] requestBlock(int pieceIndex, int blockBegin, int blockLength)	
	throws PieceNotReadable, BlockIndexOutOfBound, PieceIndexOutofBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutofBound();
		return pieces[pieceIndex].requestBlock(io, pieceIndex, blockBegin, blockLength);
	}
	
	public void updateBlock(int pieceIndex,	int blockBegin, int blockLength, byte [] data)
	throws PieceNotWritable, BlockIndexOutOfBound, PieceIndexOutofBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new PieceIndexOutofBound();
		if(pieces[pieceIndex].updateBlock(io, pieceIndex, blockBegin, blockLength, data))
			pieces[pieceIndex] = new LocalPiece((pieceIndex == pieces.length - 1));
	}
}

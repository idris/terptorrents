/**
 * 
 */
package terptorrents.models;

import java.util.BitSet;
import java.util.HashSet;

import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotReadable;
import terptorrents.exceptions.TerptorrentsModelsPieceNotWritable;
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
	throws TerptorrentsModelsPieceNotWritable, TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getBlockRangeToRequest();
		else
			throw new TerptorrentsModelsPieceNotWritable();
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
	
	public HashSet<Peer> GetPeerSet(int pieceIndex) throws TerptorrentsModelsPieceNotWritable, TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getPeerSet();
		else
			throw new TerptorrentsModelsPieceNotWritable();
	}
	
	public byte [] requestBlock(int pieceIndex, int blockBegin, int blockLength)	
	throws TerptorrentsModelsPieceNotReadable, TerptorrentsModelsBlockIndexOutOfBound, TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		return pieces[pieceIndex].requestBlock(io, pieceIndex, blockBegin, blockLength);
	}
	
	public void updateBlock(int pieceIndex,	int blockBegin, int blockLength, byte [] data)
	throws TerptorrentsModelsPieceNotWritable, TerptorrentsModelsBlockIndexOutOfBound, TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > io.getBitSet().totalNumOfPieces())
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex].updateBlock(io, pieceIndex, blockBegin, blockLength, data))
			pieces[pieceIndex] = new LocalPiece((pieceIndex == pieces.length - 1));
	}
}

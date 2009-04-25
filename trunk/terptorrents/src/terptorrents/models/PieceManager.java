/**
 * 
 */
package terptorrents.models;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
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
	private ArrayList<PeerPiece> peerPieceList;
	private ArrayList<LocalPiece> localPieceList;
	private int currentRequestBufferSize;
	private IO io;

	public PieceManager(IO io){
		this.io = io;
		IOBitSet bitMap = io.getBitSet();
		int numPieces = bitMap.totalNumOfPieces();

		Piece.setLastPieceSize(io.getPieceSize());
		Piece.setSize(io.getLastPieceSize());

		peerPieceList = new ArrayList<PeerPiece>();
		localPieceList = new ArrayList<LocalPiece>();
		currentRequestBufferSize = 0;

		pieces = new Piece[numPieces];
		for(int i = 0; i < numPieces; i++){
			pieces[i] = (bitMap.havePiece(i)) ? 
					new LocalPiece((i == numPieces - 1), i) : 
						new PeerPiece((i == numPieces - 1), i);
		}
	}

	/**
	 * This method returns a block range to request for a specified piece. 
	 * Null be will be returned for an index if the piece is a local piece or 
	 * no peer has this piece
	 * 
	 * @param pieceIndex
	 * @return
	 * @throws TerptorrentsModelsPieceNotWritable
	 * @throws TerptorrentsModelsPieceIndexOutOfBound
	 */
	public BlockRange getBlockRangeToRequest(int pieceIndex)	
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getBlockRangeToRequest();
		else
			throw new TerptorrentsModelsPieceNotWritable();
	}

	/**
	 * This method returns an array that contains the block range to request
	 * for N rarest pieces. 
	 * 
	 * @param numBlockRangeToRequest
	 * @return
	 * @throws TerptorrentsModelsPieceNotWritable
	 * @throws TerptorrentsModelsPieceIndexOutOfBound
	 */
	public BlockRange [] getNBlockRangeToRequest(int numBlockRangeToRequest)	
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsPieceIndexOutOfBound{
		Collections.sort(peerPieceList, new PeerPieceComparatorRarest());
		while(peerPieceList.get(0).getNumPeer() == 0)
			peerPieceList.remove(0);

		int numRes = Math.min(numBlockRangeToRequest, peerPieceList.size());
		BlockRange [] res = new BlockRange[numRes];

		for(int i = 0; i < numRes; i++){
			res[i] = getBlockRangeToRequest(peerPieceList.get(i).getIndex()); 
		}
		return res;
	}


	/**
	 * This method returns an array that contains the block range to request
	 * for all of the pieces. Null be will be returned for an index if the 
	 * piece is a local piece or no peer has this piece.
	 * The array index corresponds to the piece index.
	 * 
	 * @return BlockRange []
	 */
	public BlockRange [] getAllBlockRangeToRequest() {
		BlockRange [] res = new BlockRange[pieces.length];
		for(int i = 0; i < pieces.length; i++){
			try {
				res[i] = getBlockRangeToRequest(i);
			} catch (Exception e) {
				res[i] = null;
			}
		}
		return res;
	}

	public void addPeer(BitSet peerBitField, Peer peer) {
		assert peerBitField.length() == pieces.length;
		for(int i = 0; i < pieces.length; i++){
			if(pieces[i] instanceof PeerPiece && peerBitField.get(i)){
				((PeerPiece)(pieces[i])).addPeer(peer); 
				peerPieceList.add((PeerPiece)pieces[i]);
			}
		}
	}

	/**
	 * This method is called when have message is received
	 * 
	 * @param peer
	 * @param pieceIndex
	 */
	public void updatePeer(Peer peer, int pieceIndex) 	
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			((PeerPiece)(pieces[pieceIndex])).addPeer(peer);
		else
			throw new TerptorrentsModelsPieceNotWritable();
	}

	public void removePeer(Peer peer){
		for(int i = 0; i < pieces.length; i++)
			if(pieces[i] instanceof PeerPiece)
				((PeerPiece)(pieces[i])).removePeer(peer);
	}

	public HashSet<Peer> GetPeerSet(int pieceIndex) 
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getPeerSet();
		else
			throw new TerptorrentsModelsPieceNotWritable();
	}

	public byte [] requestBlock(int pieceIndex, int blockBegin, int blockLength)	
	throws TerptorrentsModelsPieceNotReadable, 
	TerptorrentsModelsBlockIndexOutOfBound, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			throw new TerptorrentsModelsPieceNotReadable();
		if(!((LocalPiece)(pieces[pieceIndex])).isBufferInRAM()){
			while(currentRequestBufferSize + pieces[pieceIndex].getSize() 
					> terptorrents.Main.MAX_REQUEST_BUFFER_SIZE)
				evictPiecesInRequestBuffer(terptorrents.Main.NUM_PIECES_TO_EVICT);
			currentRequestBufferSize += pieces[pieceIndex].getSize();
			localPieceList.add((LocalPiece)pieces[pieceIndex]);
		}
		return pieces[pieceIndex].requestBlock(io, pieceIndex, 
				blockBegin, blockLength);
	}

	public void updateBlock(int pieceIndex,	int blockBegin, int blockLength, 
			byte [] data)
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsBlockIndexOutOfBound, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex].updateBlock(io, pieceIndex, blockBegin, 
				blockLength, data)){
			//TODO this call can be expensive
			peerPieceList.remove(pieces[pieceIndex]);
			pieces[pieceIndex] = new LocalPiece(
					(pieceIndex == pieces.length - 1), 
					pieceIndex);
		}
	}

	private void evictPiecesInRequestBuffer(int numPieces){
		Collections.sort(localPieceList, new LocalPieceComparatorLRU());
		for(int i = 0; i < numPieces; i++){
			localPieceList.get(0).clearBuffer();
			localPieceList.get(0).resetNumRequest();
			currentRequestBufferSize -= localPieceList.get(0).getSize();
			localPieceList.remove(0);
		}
		for(LocalPiece localPiece:localPieceList){
			localPiece.resetNumRequest();
		}
	}
}

/**
 *
 *
 */
package terptorrents.models;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import terptorrents.exceptions.TerptorrentsIONoSuchPieceException;
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
	private static PieceManager SINGLETON = new PieceManager();
	
	private Piece [] pieces;
	private ArrayList<PeerPiece> peerPieceList;
	private ArrayList<LocalPiece> localPieceList;
	private int currentRequestBufferSize;
	private int numPieceReceived;

	public static  PieceManager getInstance() {
		return SINGLETON;
	}
	
	public static void initialize(){
		if (SINGLETON == null)
			SINGLETON = new PieceManager();
	}

	public BlockRange [] getBlockRangeToRequest(){
		int pieceIndex;
		if(numPieceReceived <= 4){
			Random random = new Random();
			pieceIndex = peerPieceList.get(random.nextInt()
					% peerPieceList.size()).getIndex();
		}else{
			Collections.sort(peerPieceList, new PeerPieceComparatorRarest());
			while(peerPieceList.get(0).getNumPeer() == 0)
				peerPieceList.remove(0);
			pieceIndex = peerPieceList.get(0).getIndex();
		}
		assert pieces[pieceIndex] instanceof PeerPiece;

		return ((PeerPiece)(pieces[pieceIndex])).getBlockRangeToRequest();
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
		if(pieces[pieceIndex] instanceof PeerPiece){
			((PeerPiece)(pieces[pieceIndex])).addPeer(peer);
			if(!peerPieceList.contains(peer))
				peerPieceList.add((PeerPiece)pieces[pieceIndex]);
		}else
			throw new TerptorrentsModelsPieceNotWritable();
	}

	/**
	 * this function can be expensive
	 * @param peer
	 */
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
		return pieces[pieceIndex].requestBlock(pieceIndex, 
				blockBegin, blockLength);
	}

	public void updateBlock(int pieceIndex, int blockBegin, byte[] data)
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsBlockIndexOutOfBound, 
	TerptorrentsModelsPieceIndexOutOfBound{
		updateBlock(pieceIndex, blockBegin, data.length, data);
	}

	public void updateBlock(int pieceIndex,	int blockBegin, int blockLength, 
			byte [] data)
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsBlockIndexOutOfBound, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex].updateBlock(pieceIndex, blockBegin, 
				blockLength, data)){
			peerPieceList.remove(pieces[pieceIndex]);
			numPieceReceived++;
			pieces[pieceIndex] = new LocalPiece(
					(pieceIndex == pieces.length - 1), 
					pieceIndex);
		}
	}
	
	private PieceManager(){
		IOBitSet bitMap = IO.getInstance().getBitSet();
		int numPieces = bitMap.totalNumOfPieces();

		Piece.setLastPieceSize(IO.getInstance().getPieceSize());
		Piece.setSize(IO.getInstance().getLastPieceSize());

		peerPieceList = new ArrayList<PeerPiece>();
		localPieceList = new ArrayList<LocalPiece>();
		currentRequestBufferSize = 0;
		numPieceReceived = 0;
		pieces = new Piece[numPieces];
		for(int i = 0; i < numPieces; i++){
			try {
				pieces[i] = (bitMap.havePiece(i)) ? 
						new LocalPiece((i == numPieces - 1), i) : 
							new PeerPiece((i == numPieces - 1), i);
			} catch (TerptorrentsIONoSuchPieceException e) {
				if(terptorrents.Main.DEBUG)
					e.printStackTrace();
			}
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

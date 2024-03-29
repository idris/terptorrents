/**
 *
 *
 */
package terptorrents.models;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import terptorrents.Main;
import terptorrents.comm.ConnectionPool;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.*;
import terptorrents.exceptions.*;
import terptorrents.io.IOBitSet;
import terptorrents.io.IO;

/**
 * @author jonli
 *
 */
public class PieceManager {
	private static PieceManager SINGLETON = null;

	private Piece [] pieces;
	private Vector<PeerPiece> peerPieceList;
	private Vector<LocalPiece> localPieceList;
	private int currentRequestBufferSize;
	private boolean endGameTiggered;

	public static  PieceManager getInstance() {
		return SINGLETON;
	}

	public static void initialize(){
		if (SINGLETON == null)
			SINGLETON = new PieceManager();
	}
	
	public static void reinitialize() {
		SINGLETON = new PieceManager();
	}


	/**
	 * @return the endGameTiggered
	 */
	public boolean isEndGameTiggered() {
		return endGameTiggered;
	}

	public synchronized Vector<BlockRange> getBlockRangeToRequest(Peer peer, 
			Collection<BlockRange> dogpile, int size) {
		Vector<BlockRange> res = new Vector<BlockRange>();
		List<PeerPiece> rarestPeerPieceList = null;
		int same = 0;
		BlockRange [] blockRanges = null;
		if(IO.getInstance().getBitSet().getNumEmptyPieces() <
				Main.NUM_OF_PIECES_LEFT_TO_TRIGGER_END_GAME_PERCENTAGE / 100.00
				* IO.getInstance().getBitSet().totalNumOfPieces()){
			if(!endGameTiggered){
				Main.dprint("End Game Is Triggered");
				endGameTiggered = true;
			}
			for(PeerPiece pp: peerPieceList){
				blockRanges = pp.getBlockRangeToRequest();
				for(int i = 0; i < blockRanges.length; i++)
					res.add(blockRanges[i]);
			}
		}else{
			Main.iprint("peerPieceList size: " + peerPieceList.size());
			synchronized (peerPieceList) {
				if(Main.ENABLE_FILE_PRIORITY_SELECTION)
					Collections.sort(peerPieceList, new PeerPieceComparatorPriority());
				else
					Collections.sort(peerPieceList, new PeerPieceComparatorRarest());
			}

			while(!peerPieceList.isEmpty() && peerPieceList.get(0).getNumPeer() == 0) {
				peerPieceList.remove(0);
			}

			Main.iprint("peerPieceList size after removals: " + peerPieceList.size());

			rarestPeerPieceList = peerPieceList.subList
			(0, Math.min(Main.NUM_PIECES_TO_INCLUDE_IN_RANDOM_LIST, peerPieceList.size()));

			if(false && Main.INFO) {
				for(PeerPiece pp: rarestPeerPieceList) {
					Main.iprint("Ranges in Piece " + pp.getIndex() + " to request: " + pp.getBlockRangeToRequest().length);
				}
			}
			if(!Main.ENABLE_FILE_PRIORITY_SELECTION)
				Collections.shuffle(rarestPeerPieceList);
			Iterator<PeerPiece> e = rarestPeerPieceList.iterator();

			int requestedBytes = 0;
			try {
				while(requestedBytes < Main.MAX_REQUEST_BLOCK_SIZE * size
						&& e.hasNext()){
					blockRanges = e.next().getBlockRangeToRequest();
					int j = 0;

					while(requestedBytes < Main.MAX_REQUEST_BLOCK_SIZE *size 
							&& j < blockRanges.length){
						if(!dogpile.contains(blockRanges[j])){
							if(((PeerPiece)pieces[blockRanges[j].getPieceIndex()]).hasPeer(peer)){
								res.add(blockRanges[j]);
								requestedBytes += blockRanges[j].getLength();
							}
						} else {
//							Main.iprint("Dogpile has " + blockRanges[j].toString());
							same++;
						}
						j++;
					}
				}
			} catch (ConcurrentModificationException ex) {
				Main.iprint("ConcurrentModificationException is caught in " +
				"PieceManager while iterating over piece List");
			}
		}

		if(res.isEmpty() && dogpile.isEmpty()) {
			Main.iprint("requesting 0");
		}
		return res;
	}

	public void addPeer(BitSet peerBitField, Peer peer) {
		assert peerBitField.length() == pieces.length;
		assert peer != null;

		for(int i = 0; i < peerBitField.size(); i++){
			if(peerBitField.get(i) && (pieces[i] instanceof PeerPiece)){
				PeerConnection pc = peer.getConnection();
				if (pc != null) pc.sendMessage(new InterestedMessage());
				break;
			}
		}

		for(int i = 0; i < pieces.length; i++){
			if((pieces[i] instanceof PeerPiece) && peerBitField.get(i)){
				((PeerPiece)(pieces[i])).addPeer(peer);
				addPeerPiece(i);
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
			if(!peer.getConnection().amInterested())
				peer.getConnection().sendMessage(new InterestedMessage());

			((PeerPiece)(pieces[pieceIndex])).addPeer(peer);
			addPeerPiece(pieceIndex);
		}else
			throw new TerptorrentsModelsPieceNotWritable("Piece " + pieceIndex + " is not a PeerPiece.");
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

	private synchronized void addPeerPiece(int index) {
		synchronized (peerPieceList) {
			if(!peerPieceList.contains((PeerPiece)pieces[index]))
				peerPieceList.add((PeerPiece)pieces[index]);
		}
	}

	public Vector<Peer> GetPeerSet(int pieceIndex) 
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex] instanceof PeerPiece)
			return ((PeerPiece)(pieces[pieceIndex])).getPeerSet();
		else
			throw new TerptorrentsModelsPieceNotWritable("Piece " + pieceIndex + " is not a PeerPiece.");
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

	public void updateBlock(int pieceIndex,	int blockBegin, int blockLength, 
			byte [] data)
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsBlockIndexOutOfBound, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(!(pieces[pieceIndex] instanceof PeerPiece)) 
			throw new TerptorrentsModelsPieceNotWritable("Piece " + pieceIndex + " is not a PeerPiece.");

		PeerPiece peerPiece = (PeerPiece)pieces[pieceIndex];
		if(peerPiece.updateBlock(pieceIndex, blockBegin, 
				blockLength, data)){
			// tell RequestManager that we now have this piece
			RequestManager.getInstance().pieceComplete(pieceIndex);

			// change this piece into a LocalPiece (first remove the PeerPiece)
			synchronized (peerPieceList) {
				while(peerPieceList.remove(peerPiece)); // in case there are duplicates, keep removing the duplicates from the list
			}
			pieces[pieceIndex] = new LocalPiece(
					(pieceIndex == pieces.length - 1), 
					pieceIndex);

			/*send have messages*/
			for(PeerConnection conn : ConnectionPool.getInstance().
					getConnections()){
				if(conn != null){
					conn.sendMessage(new HaveMessage(pieceIndex));
				}
			}
			Enumeration<Peer> ps = (peerPiece).getPeerSet().elements();
			Peer peer;
			while(ps.hasMoreElements()){
				peer = ps.nextElement();
				boolean peerHaveOtherPiece = false;
				PeerPiece pp;
				for(int i = 0; i < peerPieceList.size(); i++){
					pp = peerPieceList.get(i);
					if(pp.hasPeer(peer)){
						Main.iprint("PEER " + peer.toString() + " also has " + pp.getIndex());
						peerHaveOtherPiece = true;
						break;
					}
				}
				if(!peerHaveOtherPiece){
					peer.getConnection().sendMessage(new NotInterestedMessage());
				}
			}
		}
	}

	private PieceManager(){
		IOBitSet bitMap = IO.getInstance().getBitSet();
		int numPieces = bitMap.totalNumOfPieces();

		Piece.setLastPieceSize(IO.getInstance().getLastPieceSize());
		Piece.setSize(IO.getInstance().getPieceSize());

		peerPieceList = new Vector<PeerPiece>();
		localPieceList = new Vector<LocalPiece>();
		currentRequestBufferSize = 0;
		endGameTiggered = false;
		pieces = new Piece[numPieces];
		for(int i = 0; i < numPieces; i++){
			try {
				try {
					pieces[i] = (bitMap.havePiece(i)) ? 
							new LocalPiece((i == numPieces - 1), i) : 
								(Main.ENABLE_FILE_PRIORITY_SELECTION && 
										IO.getInstance().getHighPriorityPieces().contains(i)? 
										new PeerPiece((i == numPieces - 1), i, 100) : 
											new PeerPiece((i == numPieces - 1), i, 0));
				} catch (IODeselectedPieceException e) {
					pieces[i] = new LocalPiece((i == numPieces - 1), i) ;
				}
			} catch (TerptorrentsIONoSuchPieceException e) {
				Main.dprint("PieceMangager: " + e.toString());
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

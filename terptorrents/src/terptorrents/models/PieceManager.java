/**
 *
 *
 */
package terptorrents.models;

import java.util.BitSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Set;
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
	private int numPieceReceived;
	private boolean endGameTiggered;

	public static  PieceManager getInstance() {
		return SINGLETON;
	}

	public static void initialize(){
		if (SINGLETON == null)
			SINGLETON = new PieceManager();
	}


	/**
	 * @return the endGameTiggered
	 */
	public boolean isEndGameTiggered() {
		return endGameTiggered;
	}

	public Vector<BlockRange> getBlockRangeToRequest(Peer peer, 
			Set<BlockRange> RequestedBlock, int size) throws
			TerptorrentsModelsCanNotRequstFromThisPeer{
		PeerConnection conn = peer.getConnection();
		if(conn == null) 
			throw new TerptorrentsModelsCanNotRequstFromThisPeer
			("Disconnected Peer");

		if(conn.peerChoking() || !conn.amInterested())
			throw new TerptorrentsModelsCanNotRequstFromThisPeer(
			"Peer is choking us or We are not intersted");

		Vector<BlockRange> res = new Vector<BlockRange>();
		if(numPieceReceived + Main.NUM_OF_PIECES_LEFT_TO_TRIGGER_END_GAME >= 
			IO.getInstance().getBitSet().totalNumOfPieces()){
			if(!endGameTiggered){
				System.out.println("End Game Is Triggered");
				endGameTiggered = true;
			}
			for(PeerPiece pp: peerPieceList){
				BlockRange [] blockRanges = pp.getBlockRangeToRequest();
				for(int i = 0; i < blockRanges.length; i++)
					res.add(blockRanges[i]);
			}
		}else{
			Collections.sort(peerPieceList, new PeerPieceComparatorRarest());
			while(!peerPieceList.isEmpty() && peerPieceList.get(0).getNumPeer() == 0)
				peerPieceList.remove(0);
			
			Vector<PeerPiece> rarestPeerPieceList = new Vector<PeerPiece>();
			Enumeration<PeerPiece> e = peerPieceList.elements();
			PeerPiece firstRarestPiece = e.nextElement();
			
			while(e.hasMoreElements()){
				PeerPiece currentPeerPiece = e.nextElement();
				if(firstRarestPiece.getPeerSet().size() ==
					currentPeerPiece.getPeerSet().size())
					rarestPeerPieceList.add(currentPeerPiece);
				else
					break;
			}
			
			Collections.shuffle(rarestPeerPieceList);
			e = rarestPeerPieceList.elements();
			
			int requestedBytes = 0;
			try {
				while(requestedBytes < Main.MAX_REQUEST_BLOCK_SIZE * size
						&& e.hasMoreElements()){
					BlockRange [] blockRanges = e.nextElement().getBlockRangeToRequest();
					int j = 0;
					while(requestedBytes < Main.MAX_REQUEST_BLOCK_SIZE *size 
							&& j < blockRanges.length){
						if(!RequestedBlock.contains(blockRanges[j])){
							res.add(blockRanges[j]);
							requestedBytes += blockRanges[j].getLength();
						}
						j++;
					}
				}
			} catch (ConcurrentModificationException ex) {
				Main.dprint("ConcurrentModificationException is caugh in " +
				"PieceManager while iterating over piece List");
			}
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
			if(!peer.getConnection().amInterested())
				peer.getConnection().sendMessage(new InterestedMessage());

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

	public Vector<Peer> GetPeerSet(int pieceIndex) 
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

	public void updateBlock(int pieceIndex,	int blockBegin, int blockLength, 
			byte [] data)
	throws TerptorrentsModelsPieceNotWritable, 
	TerptorrentsModelsBlockIndexOutOfBound, 
	TerptorrentsModelsPieceIndexOutOfBound{
		if(pieceIndex < 0 || pieceIndex > pieces.length)
			throw new TerptorrentsModelsPieceIndexOutOfBound();
		if(pieces[pieceIndex].updateBlock(pieceIndex, blockBegin, 
				blockLength, data)){

			/*send have messages*/
			for(PeerConnection conn : ConnectionPool.getInstance().
					getConnections()){
				if(conn != null){
					conn.sendMessage(new HaveMessage(pieceIndex));
				}
			}
			Enumeration<Peer> ps = ((PeerPiece)(pieces[pieceIndex])).getPeerSet().elements();
			Peer peer;
			while(ps.hasMoreElements()){
				peer = ps.nextElement();
				boolean peerHaveOtherPiece = false;
				PeerPiece pp;
				for(int i = 0; i < peerPieceList.size(); i++){
					pp = peerPieceList.get(i);
					if(pp.hasPeer(peer)){
						peerHaveOtherPiece = true;
						break;
					}
				}
				if(!peerHaveOtherPiece){
					peer.getConnection().sendMessage(new NotInterestedMessage());
				}
			}
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

		Piece.setLastPieceSize(IO.getInstance().getLastPieceSize());
		Piece.setSize(IO.getInstance().getPieceSize());

		peerPieceList = new Vector<PeerPiece>();
		localPieceList = new Vector<LocalPiece>();
		currentRequestBufferSize = 0;
		numPieceReceived = 0;
		endGameTiggered = false;
		pieces = new Piece[numPieces];
		for(int i = 0; i < numPieces; i++){
			try {
				try {
					pieces[i] = (bitMap.havePiece(i)) ? 
							new LocalPiece((i == numPieces - 1), i) : 
								new PeerPiece((i == numPieces - 1), i);
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

package terptorrents.models;

import java.io.IOException;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map.Entry;

import terptorrents.exceptions.TerptorrentsIONoSuchPieceException;
import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotReadable;
import terptorrents.io.IO;
import terptorrents.*;

public class PeerPiece extends Piece {
	private HashSet<Peer> peerSet;
	private TreeMap<Integer, Integer> freeBlock; 
	private byte[] data;

	public PeerPiece(boolean isLastPiece, int index){
		super(isLastPiece, index);
		peerSet = new HashSet<Peer>();
		data = new byte[getSize()];
		freeBlock = new TreeMap<Integer, Integer>();
		initFreeBlock();
	}

	public synchronized int getNumPeer(){
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
	public synchronized BlockRange [] getBlockRangeToRequest(){
		BlockRange [] res = new BlockRange [freeBlock.size()];

		TreeMap<Integer, Integer> mergedBlock = mergeBlock();
		int i = 0;
		for(int key : mergedBlock.keySet()){
			res [i] = new BlockRange(key, mergedBlock.get(key), getIndex());
			i++;
		}
		return res;
	}

	public synchronized boolean hasPeer(Peer peer){
		return peerSet.contains(peer);
	}
	
	public synchronized void addPeer(Peer newPeer){
		peerSet.add(newPeer);
	}

	public synchronized void removePeer(Peer peer){
		if(peerSet.contains(peer))
			peerSet.remove(peer);
	}

	public synchronized HashSet<Peer> getPeerSet(){
		return peerSet;
	}

	@Override
	public byte [] requestBlock(int pieceIndex, int blockBegin, 
			int blockLength) throws TerptorrentsModelsPieceNotReadable{
		throw new TerptorrentsModelsPieceNotReadable();
	}

	@Override
	public synchronized boolean updateBlock(int pieceIndex,
			int blockBegin, int blockLength, byte [] data) 
	throws TerptorrentsModelsBlockIndexOutOfBound {
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new TerptorrentsModelsBlockIndexOutOfBound();

		Main.dprint("BlockReceived. PieceIndex: " + pieceIndex + 
				" blockBegin: " + blockBegin + " blockLength: " + 
				blockLength);
		
		int oldBlockLength = blockLength;
		int oldBlockBegin = blockBegin;
		
		while(blockLength > 0){
			Entry<Integer, Integer> entry = freeBlock.floorEntry(blockBegin);
			if(entry == null || entry.getKey() + entry.getValue() <= blockBegin){
				entry = freeBlock.ceilingEntry(blockBegin);
				if(entry == null)
					break;
				blockBegin = entry.getKey();
				blockLength -= entry.getKey() - blockBegin;
			}else{
				int currentBlockBegin = entry.getKey();
				int currentBlockLength = entry.getValue();
				freeBlock.remove(currentBlockBegin);
				int firstBegin = currentBlockBegin; 
				int firstLength = blockBegin - currentBlockBegin;
				int secondBegin = blockBegin + blockLength; 
				int secondLength = ((blockBegin + blockLength) 
						>= (currentBlockBegin + currentBlockLength) ? 
						0 : (currentBlockBegin + currentBlockLength - 
								(blockBegin + blockLength))); 
				if(firstLength > 0)
					freeBlock.put(firstBegin, firstLength);
				if(secondLength > 0)
					freeBlock.put(secondBegin, secondLength);
				blockBegin = currentBlockBegin + currentBlockLength;
				blockLength -= currentBlockLength - firstLength - secondLength;
			}
		}
		boolean res = false;
		
		//System.arraycopy(src, srcPos, dest, destPos, length)
		System.arraycopy(data, 0, this.data, oldBlockBegin, oldBlockLength);
		if(Have_Piece()){
			/*
	        try {
				FileOutputStream writer = new FileOutputStream("pieceDump.txt");
				writer.write(this.data);
				writer.close();
	        } catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			
			Main.dprint("Received Piece " + pieceIndex);
			
			try {
				if(IO.getInstance().writePiece(pieceIndex, this.data)){
					res = true;
				}else{
					initFreeBlock();
				}
			} catch (IOException e) {
				if(terptorrents.Main.DEBUG)
					e.printStackTrace();
			} catch (TerptorrentsIONoSuchPieceException e) {
				if(terptorrents.Main.DEBUG)
					e.printStackTrace();
			}
		}
		return res;
	}	

	private boolean Have_Piece(){
		return freeBlock.isEmpty();
	}

	private void initFreeBlock(){
		freeBlock.clear();
		int end = getSize() / Main.MAX_REQUEST_BLOCK_SIZE;
		
		for(int i = 0; i < end; i++)
			freeBlock.put(i * Main.MAX_REQUEST_BLOCK_SIZE, 
					Main.MAX_REQUEST_BLOCK_SIZE);

		if(getSize() % Main.MAX_REQUEST_BLOCK_SIZE != 0)
			freeBlock.put(end * Main.MAX_REQUEST_BLOCK_SIZE, 
					getSize() % Main.MAX_REQUEST_BLOCK_SIZE);
	}

	private TreeMap<Integer, Integer> mergeBlock(){
		TreeMap<Integer, Integer> res = freeBlock;
		return res;
	}
}

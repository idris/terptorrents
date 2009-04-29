package terptorrents.models;

import java.io.ByteArrayOutputStream;
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
	private ByteArrayOutputStream data;

	public PeerPiece(boolean isLastPiece, int index){
		super(isLastPiece, index);
		peerSet = new HashSet<Peer>();
		data = new ByteArrayOutputStream(getSize());
		freeBlock = new TreeMap<Integer, Integer>();
		initFreeBlock();
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
	public BlockRange [] getBlockRangeToRequest(){
		BlockRange [] res = new BlockRange [freeBlock.size()];

		TreeMap<Integer, Integer> mergedBlock = mergeBlock();
		int i = 0;
		for(int key : mergedBlock.keySet()){
			res [i] = new BlockRange(key, mergedBlock.get(key), getIndex());
		}
		return res;
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
	public byte [] requestBlock(int pieceIndex, int blockBegin, 
			int blockLength) throws TerptorrentsModelsPieceNotReadable{
		throw new TerptorrentsModelsPieceNotReadable();
	}

	@Override
	public boolean updateBlock(int pieceIndex,
			int blockBegin, int blockLength, byte [] data) 
	throws TerptorrentsModelsBlockIndexOutOfBound {
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new TerptorrentsModelsBlockIndexOutOfBound();

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
				int secondLength = ((blockBegin + blockLength) >= (currentBlockBegin + currentBlockLength) ? 
						0 : (currentBlockBegin + currentBlockLength - (blockBegin + blockLength))); 
				if(firstLength > 0)
					freeBlock.put(firstBegin, firstLength);
				if(secondLength > 0)
					freeBlock.put(secondBegin, secondLength);
				blockBegin = currentBlockBegin + currentBlockLength;
				blockLength -= currentBlockLength - firstLength - secondLength;
			}
		}
		boolean res = false;
		this.data.write(data, blockBegin, blockLength);
		if(Have_Piece()){
			try {
				if(IO.getInstance().writePiece(pieceIndex, this.data.toByteArray())){
					try {
						this.data.close();
					} catch (IOException e) {
						if(terptorrents.Main.DEBUG)
							e.printStackTrace();
					}
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
		for(int i = 0; i < getSize(); i++){
			freeBlock.put(i * Main.MAX_REQUEST_BLOCK_SIZE, 
					Main.MAX_REQUEST_BLOCK_SIZE);
		}
	}

	private TreeMap<Integer, Integer> mergeBlock(){
		TreeMap<Integer, Integer> res = freeBlock;
		return res;
	}
}

package terptorrents.models;

import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotReadable;
import terptorrents.exceptions.TerptorrentsModelsPieceNotWritable;

abstract public class Piece {
	private static int size;
	private static int lastPieceSize;
	private boolean isLastPiece;
	private int index;
	
	public Piece(boolean isLastPiece, int index){
		this.index = index;
		this.isLastPiece = isLastPiece;
	}

	/**
	 * This function must be called before calling the constructor
	 * */
	public static void setLastPieceSize(int size){
		Piece.lastPieceSize = size;
	}
	
	/**
	 * This function must be called before calling the constructor
	 * */
	public static void setSize(int size){
		Piece.size = size;
	}
	
	public boolean isLastPiece(){
		return isLastPiece;
	}
	
	public int getIndex(){
		return index;
	}
	
	public int getSize(){
		return (isLastPiece)? lastPieceSize : size;
	}
	
	public abstract byte [] requestBlock(int pieceIndex, 
			int blockBegin, int blockLength)	
	throws TerptorrentsModelsPieceNotReadable, TerptorrentsModelsBlockIndexOutOfBound;
	
	public abstract boolean updateBlock(int pieceIndex, int blockBegin, int blockLength,
			byte[] data) throws TerptorrentsModelsBlockIndexOutOfBound,
			TerptorrentsModelsPieceNotWritable;
}

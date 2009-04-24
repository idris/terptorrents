package terptorrents.models;

import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotReadable;
import terptorrents.exceptions.TerptorrentsModelsPieceNotWritable;
import terptorrents.io.IO;

abstract public class Piece {
	private static int size;
	private static int lastPieceSize;
	private boolean isLastPiece;

	
	public Piece(boolean isLastPiece){
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
	
	public int getSize(){
		return (isLastPiece)? lastPieceSize : size;
	}
	
	public abstract byte [] requestBlock(IO io, int pieceIndex, 
			int blockBegin, int blockLength)	
	throws TerptorrentsModelsPieceNotReadable, TerptorrentsModelsBlockIndexOutOfBound;
	
	public abstract boolean updateBlock(IO io, int pieceIndex,
			int blockBegin, int blockLength, byte [] data)
	throws TerptorrentsModelsPieceNotWritable, TerptorrentsModelsBlockIndexOutOfBound;
}

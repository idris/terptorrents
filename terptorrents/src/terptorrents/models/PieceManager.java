/**
 * 
 */
package terptorrents.models;

import java.util.BitSet;

import terptorrents.exceptions.PieceNotWritable;

/**
 * @author jonli
 *
 */
public class PieceManager {
	private Piece [] pieces;
	private BitSet bitMap;
	
	public PieceManager(int numPieces, BitSet bitMap){
		pieces = new Piece[numPieces];
		this.bitMap = bitMap;
		for(int i = 0; i < numPieces; i++){
			if(bitMap.get(i))
				pieces[i] = new LocalPiece((i == numPieces - 1));
			else
				pieces[i] = new PeerPiece((i == numPieces - 1));
		}
	}
	
	public void update_piece(int pieceIndex, int begin, int lengh, byte[] data) 
	throws PieceNotWritable{
	}
}

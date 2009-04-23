package terptorrents.io;
import java.util.*;

public interface BitMask {
	
	/* returns true if piece is available (downloaded) */
	public boolean havePiece(int index);
	
	/* returns total number of pieces in a current torrent */
	public int totalNumOfPieces();
	
	/* returns number of pieces that needs to be downloaded/empty */
	public int emptyPieces();
	
	/* returns a set of indexes of empty pieces (that need to be downloaded)
	 * Set represents the current snapshot of the file.
	 * havePiece() method should be called prior to downloading a piece
	 * to make sure that piece have not been downloaded yet
	 */ 
	public Set<Integer> getEmptyPiecesSet();
	
}

package terptorrents.io;
import java.util.*;

public interface IOBitSet {
	
	/* returns true if piece is available (downloaded) */
	public boolean havePiece(int index);
	
	/* returns total number of pieces in a current torrent */
	public int totalNumOfPieces();
	
	/* returns number of pieces that needs to be downloaded/empty */
	public int getNumEmptyPieces();
	
	/* returns an Iterator over the empty pieces that needs to be downloaded
	 * call to remove() method will have no effect
	 */ 
	public Iterator<Integer> getEmptyPiecesIterator();
	
	/* returns java BitSet representing pieces that IO has. Returned class
	 * is not synchronized and should be only used for building some
	 * messages
	 */
	public BitSet getUnsyncBitSet();

}

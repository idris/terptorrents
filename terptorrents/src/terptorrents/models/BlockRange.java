/**
 * 
 */
package terptorrents.models;

/**
 * @author jonli
 *
 */
public class BlockRange {
	private int begin;
	private int length;
	private int pieceIndex;
	
	public BlockRange(int begin, int length, int pieceIndex){
		this.begin = begin;
		this.length = length;
		this.pieceIndex = pieceIndex;
	}

	/**
	 * @return the begin
	 */
	public int getBegin() {
		return begin;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @return the pieceIndex
	 */
	public int getPieceIndex() {
		return pieceIndex;
	}
	
	
	
	
}

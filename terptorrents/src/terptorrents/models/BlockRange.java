/**
 * 
 */
package terptorrents.models;

/**
 * @author jonli
 *
 */
public class BlockRange {
	public int begin;
	public int length;
	public int pieceIndex;
	
	public BlockRange(int begin, int length, int pieceIndex){
		this.begin = begin;
		this.length = length;
		this.pieceIndex = pieceIndex;
	}
}

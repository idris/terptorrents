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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + begin;
		result = prime * result + length;
		result = prime * result + pieceIndex;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlockRange other = (BlockRange) obj;
		if (begin != other.begin)
			return false;
		if (length != other.length)
			return false;
		if (pieceIndex != other.pieceIndex)
			return false;
		return true;
	}

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

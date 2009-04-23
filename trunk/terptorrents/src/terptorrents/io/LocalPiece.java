package terptorrents.io;


/* A container that represents a piece of data
 * Brain can request some piece of file from IO
 * and can give a piece of just downloaded file to write
 * to a local file
 */
public class LocalPiece {
	
	private byte[] data;
	
	/* puts some piece of file into container */
	public LocalPiece(byte[] data) {
		this.data = data;
	}
	
	/* returns the whole payload of this piece */
	public byte[] getData() {
		return data;
	}
	
	/* returns the specified offset of the piece 
	 * conditions: start > 0, end > 0, end > start, start < data.size, 
	 * end < data.size
	 * Otherwise index out of bounds exception is thrown	
	 */
	public byte[] getBlock(int start, int end) {
		return null;
	}
	
	public void putData(byte[] data) {
		this.data = data;
	}

}

package terptorrents.io;

public class IO {
	
	/* returns a COPY of the piece that is stored in the file
	 * for upload by brain
	 */	
	public byte[] getPiece(int i) {
		return null;
	}
	
	/* Computes SHA1 of the piece and writes it into the file
	 * Returns: false if SHA1 does not match with SHA1 in MetaFile
	 */
	public boolean writePiece(int index, byte[] piece) {
		return false;
	}
	
	/* returns a bitmask of pieces that are available fro upload */
	public BitSet getBitMask() {
		return null;
	}
	
	/* return true is all pieces are available in a file */
	public boolean isComplete() {
		return false;
	}
}
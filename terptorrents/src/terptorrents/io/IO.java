package terptorrents.io;

import java.io.*;
import java.util.*;

public class IO {
	
	private RandomAccessFile files[];
	
	private Object lock = new Object();
	
	public IO() {

	}
	
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
	
	/* returns a bit mask of pieces that are available for upload */
	public IOBitSet getBitSet() {
		return new MyIOBitSet();
	}
	
	/* return true is all pieces are available in a file */
	public boolean isComplete() {
		return false;
	}
	
	/* returns size of all pieces except the last one, which might
	 * be irregular
	 */
	public int getPieceSize() {
		return 0;
	}
	
	/* returns irregular piece size */
	public int getLastPieceSize() {
		return 0;
	}
	
	private class MyIOBitSet implements IOBitSet {

		public Iterator<Integer> getEmptyPiecesIterator() {
			return null;
		}

		public int getNumEmptyPieces() {
			// TODO Auto-generated method stub
			return 0;
		}

		public BitSet getUnsyncBitSet() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean havePiece(int index) {
			// TODO Auto-generated method stub
			return false;
		}

		public int totalNumOfPieces() {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}

}

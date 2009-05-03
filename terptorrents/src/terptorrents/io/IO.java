package terptorrents.io;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import metainfo.MetaFile;

import terptorrents.Main;
import terptorrents.exceptions.*;

/* Thread safe class that represents IO layer for Torrent Client
 * Creates files specified in .torrent file on local machine 
 * and provides read/write pieces functionality
 */
public class IO {
	
	/* SINGLETON */
	private static IO instance = null;
	private static boolean DEBUG = false;
	private RandomAccessFile files[];
	private final int pieceSize;
	private final int irregPieceSize;
	private final MessageDigest digest;
	private final Map<Integer, byte[]> pieceHashes;
	private volatile boolean mask[];
	private MyIOBitSet ioBitSet;

	
	private IO(MetaFile m) throws IOException {
		dprint("Starting...");	
		/* create folders structure */
		this.ioBitSet = new MyIOBitSet();
		prepareFolders(m);
		/* create dummy files to download, if necessary */
		createFiles(m);
		if (files.length == 0) 
			throw new IOException("File list to download is empty");
		this.pieceSize = m.getPieceLength().intValue();
		/* calculate irregular Piece size */
		long total = 0;
		for (int i = 0; i < files.length; i++)
			total += files[i].length();
		this.irregPieceSize = (int) (total % pieceSize);
		/* instantiate message digest function SHA1*/	    
	    try {
	    	dprint("Instantiating SHA1...");
	    	digest = MessageDigest.getInstance("SHA");
	    } catch(NoSuchAlgorithmException nsa) {
	    	throw new InternalError(nsa.toString());
	    }
	    dprint("Reading hashes from MetaFile...");
	    pieceHashes = m.getSHAHashes();
	    /* check integrity */
	    long totalLength = 0;
	    for (int i = 0; i < files.length; i++) 
	    	totalLength += files[i].length();
	    int numPieces = (int) Math.ceil(((double)totalLength)/pieceSize);
	    if (pieceHashes.size() != numPieces )
	    	throw new IOException("Number of hashes #" + pieceHashes.size() + 
	    			"do not match with number of pieces in a file: " + numPieces);
	    /* check files for integrity, and see what parts need to be downloaded */
	    mask = new boolean[numPieces];
	    checkFilesIntegrity();
	    /* it was set to false just to remove some prints caused by constructor */
	    DEBUG = Main.DEBUG;
	}
	
	/* checks pieces in a file against SHA1 and mark mask[] if piece needs
	 * to be downloaded
	 */
	private void checkFilesIntegrity() throws IOException {
		dprint("Checking file(s) integrity...");
		byte[] piece;
		dprint("Marking pieces to download: ");
		for (int i = 0; i < mask.length; i++) {
			try {
				piece = this.getPiece(i);
				digest.update(piece);
				byte[] hash = digest.digest();				
				if (!Arrays.equals(hash, this.pieceHashes.get(i))) {
					if (Main.DEBUG) System.out.print(i + "  ");
					mask[i] = false;
				} else {
					mask[i] = true;
				}
			} catch (TerptorrentsIONoSuchPieceException e) {
				dprint("Integrity Checking failed. Reason: " + e.getMessage());
				throw new InternalError("checkFilesIntegrity() function failed. Requested piece does not exists");
			}
		}
		dprint("");
	}
	
	private void prepareFolders(MetaFile m) {
		Set<String> paths = m.getFileFolders();
		if (paths.isEmpty()) return;
		File f;
		dprint("Preparing folders srtucture");
		for (String path : paths) {
			f = new File(path);
			if (!f.exists()) f.mkdirs();
		}
	}
	
	/* Creates files specified in a MetaFile on disk. Fills them
	 * with 0s to restore original length
	 * if file exists but have wrong length, it will be removed
	 * and recreated 
	*/
	private void createFiles(MetaFile m) throws IOException {
		List<String> filenames = m.getFilenames();
		Map<String, Long> lengths = m.getFileLengths();
		File f;
		RandomAccessFile newFile;
		//LinkedList<RandomAccessFile> filesList = new LinkedList<RandomAccessFile>();
		long length;
		this.files = new RandomAccessFile[filenames.size()];
		String fileName;
		for (int i =0; i < filenames.size(); i++) {
			fileName = filenames.get(i);
			f = new File(fileName);
			length = lengths.get(fileName);
			/* if file does not exists, create and fill with zeros */
			/* if it is exists, check that size matches */
			if (!f.exists()) {
				dprint("Creating new file: '" + fileName + "' Size: " + length);
				f.createNewFile();
				/* set original file length */
				newFile = new RandomAccessFile(fileName, "rw");
				newFile.setLength(length);				
			} else {
				newFile = new RandomAccessFile(fileName, "rw");
				if (newFile.length() != length) {
					dprint("Existing file '" + fileName + "' has wrong size = " + f.length() + 
							" Adjusting the size to " + length);
					newFile.setLength(length);
					newFile.close();
				}
			}
			dprint("Adding new file #" + i + ": " + fileName);
			this.files[i] = newFile;
		}
	}
	
	/* returns SINGLETON instance of IO. If not instantiated, runtime exception is thrown */
	public static IO getInstance() {
		if (instance == null)
			throw new TerptorrentsIONotInstanciatedException("Instantiate IO by calling instantiate()");
		return instance;
	}
	
	/* Instantiates IO. If called twice, second call is ignored */
	public static void instantiate(MetaFile m) throws IOException {
		if (instance == null)
			instance = new IO(m);
	}
	
	/* returns a COPY of the piece that is stored in the file
	 * for upload by brain
	 */	
	public synchronized byte[] getPiece(int i) throws IOException, TerptorrentsIONoSuchPieceException {
		if (DEBUG) dprint("Piece #" + i + " requested");
		if (i < 0 || i >= mask.length) 
			throw new TerptorrentsIONoSuchPieceException("Requested index:" + i + 
					" is out of bounds");
		long startOffset = i*pieceSize;
		long endOffset = startOffset + pieceSize;
		LongContainer cont = new LongContainer();
		int startFileIndex = findFile(startOffset, cont);
		startOffset -= cont.l;
		cont = new LongContainer();
		int endFileIndex = findFile(endOffset, cont);
		endOffset -= cont.l;

		/* possible cases:
		 * 1. No such piece
		 * 2. Piece is entirely inside one file
		 * 3. Irregular piece
		 * 4. Piece is on the boundary of 2 files (There can be multiple files between startOffset and endOffset)
		 */
		/* 1 */
		if (startFileIndex == -1 && endFileIndex == -1) 
			throw new TerptorrentsIONoSuchPieceException("Requested piece #" + i + " does not exists");
		byte[] result;
		int numRead = 0;
		/* 2 */
		if (startFileIndex == endFileIndex) {
			result = new byte[pieceSize];
			RandomAccessFile startFile = files[startFileIndex];
			startFile.seek(startOffset);
			numRead = startFile.read(result);
		/* 3 */
		} else if (startFileIndex != -1 && endFileIndex == -1){
			RandomAccessFile startFile = files[startFileIndex];
			result = new byte[irregPieceSize];
			/* check if piece entirely inside the last file */
			if (startFileIndex == files.length - 1) {				
				startFile.seek(startOffset);
				numRead = startFile.read(result);
			} else {
				/* last couple files fit inside irregular piece */
				numRead = 0;
				/* read bytes from the start file */
				numRead += read(startFileIndex, (int)startOffset, TILL_END, result, numRead);
				/* read bytes from the rest of the files to the right */
				for (int indx = startFileIndex + 1; indx < files.length; indx++) 
					numRead += read(indx, 0, TILL_END, result, numRead);
			}
		/* 4 */
		} else {
			result = new byte[pieceSize];
			numRead = 0;
			/* copy part of the piece from the file on the left */
			numRead += read(startFileIndex, (int)startOffset, TILL_END, result, numRead);
			/* copy content of the files between startFile and endFile */
			for (int indx = startFileIndex + 1; indx < endFileIndex; indx++) {
				numRead += read(indx, 0, TILL_END, result, numRead);
			}			
			/* copy part of the piece from the file on the right */
			numRead += read(endFileIndex, 0, pieceSize - numRead, result, numRead);
		}
		if (numRead < 0 || numRead != result.length)
			throw new InternalError("Error in getting piece");
		return result;
	}
	
	//return number of read bytes
	private static final int TILL_END = 0;
	private int read(int file, int offset, int bytesToRead, byte[] result, int resultOffset) throws IOException {
		RandomAccessFile f = files[file];
		byte[] tmp;
		if (bytesToRead == TILL_END) {
			tmp = new byte[(int)(f.length() - offset)];
		} else tmp = new byte[bytesToRead];
		f.seek(offset);
		int tmpNumRead = f.read(tmp);
		System.arraycopy(tmp, 0, result, resultOffset, tmpNumRead);
		return tmpNumRead;
	}

	
	/* return file which have specified byte of data */
	private class LongContainer{ long l = 0;};
	private int findFile(long offset, LongContainer cont) throws IOException {
		long totalLength = 0;
		for (int i = 0; i < files.length; i++) {
			totalLength += files[i].length();
			if (offset < totalLength)
				return i;
			/* get total length of the files prior the one I should read from
			 * so I can adjust offset later
			 */
			cont.l += files[i].length();
		}
		return -1;			
	}
	
	
	/* Computes SHA1 of the piece and writes it into the file
	 * Returns: false if SHA1 does not match with SHA1 in MetaFile
	 */
	public synchronized boolean writePiece(int i, byte[] piece) throws IOException, TerptorrentsIONoSuchPieceException {
		if (DEBUG) dprint("Writing piece #" + i + " Size: " + piece.length);
		if (i < 0 || i >= mask.length) 
			throw new TerptorrentsIONoSuchPieceException("Requested index:" + i + 
					" is out of bounds");
		if (piece.length > this.pieceSize)
			throw new TerptorrentsIONoSuchPieceException("writePiece() Given piece is > than pieceSize");
		long startOffset = i*pieceSize;
		long endOffset = startOffset + pieceSize;
		LongContainer cont = new LongContainer();
		int startFileIndex = findFile(startOffset, cont);
		startOffset -= cont.l;
		cont = new LongContainer();
		int endFileIndex = findFile(endOffset, cont);
		endOffset -= cont.l;	
		/* possible cases:
		 * 1. No such piece
		 * 2. Piece is entirely inside one file
		 * 3. Irregular piece
		 * 4. Piece is on the boundary of 2 files (there can be smaller files in between)
		 */
		/* 1 */
		if (startFileIndex == -1 && endFileIndex == -1) 
			throw new TerptorrentsIONoSuchPieceException("Requested piece #" + i + 
					" does not exists");
		
		/* calculate SHA1 on this piece for integrity checking*/
		digest.update(piece);
		byte[] hash = digest.digest();
		if (!Arrays.equals(hash, pieceHashes.get(i))) {
			dprint("Hash function of given piece #" + i + " does not match with MetaFile");
			return false;
		}
		/* *********** */
		
		/* 2 */
		if (startFileIndex == endFileIndex) {
			RandomAccessFile startFile = files[startFileIndex];
			startFile.seek(startOffset);
			startFile.write(piece);
		/* 3 */
		} else if (startFileIndex != -1 && endFileIndex == -1){
			dprint("Writing irregular piece #" + i);
			if (piece.length != this.irregPieceSize)
				throw new TerptorrentsIONoSuchPieceException
					("writePiece(): Irregular piece size does not match");
			RandomAccessFile startFile = files[startFileIndex];
			/* there might be multiple files that fit in irregular piece */
			/* check if startFileIndex is the last file in the list */
			if (startFileIndex == files.length - 1) {
				/* if it is the last file, just write in it whole piece */
				startFile.seek(startOffset);
				startFile.write(piece);
			} else {
				/* irregular piece must be split and written in multiple files */
				int numWritten = 0;
				/* write into firstFile */
				numWritten += write(startFileIndex, (int)startOffset, TILL_END, piece, numWritten);
				/* write pieces into the files to the right of the startFile */
				for (int indx = startFileIndex + 1; indx < files.length; indx++)
					numWritten += write(indx, 0, TILL_END, piece, numWritten);
				if (numWritten != piece.length)
					throw new InternalError("Error writing IRREGULAR piece");
			}
		/* 4 */
		} else {
			int numWritten = 0;
			/* write some bytes into the left file */
			numWritten += write(startFileIndex, (int)startOffset, TILL_END, piece, numWritten);
			/* write bytes into the files in-between startFile and EndFile */
			for (int indx = startFileIndex + 1; indx < endFileIndex; indx++) {
				numWritten += write(indx, 0, TILL_END, piece, numWritten);
			}
			/* write remaining bytes to the right file */
			numWritten += write(endFileIndex, 0, (piece.length - numWritten), piece, numWritten);
			if (numWritten != piece.length)
				throw new InternalError("Error writing a piece");
			
		}
		mask[i] = true;
		return true;
	}
	
	private int write(int file, int offset, int len, byte[] piece, int pieceOffset) throws IOException {
		byte tmp[];
		RandomAccessFile f = files[file];
		if (len == TILL_END) {
			tmp = new byte[(int)(f.length() - offset)];
		} else tmp = new byte[len];
		System.arraycopy(piece, pieceOffset, tmp, 0, tmp.length);
		files[file].seek(offset);
		files[file].write(tmp);
		return tmp.length;
	}
	
	/* returns a bit mask of pieces that are available for upload */
	public IOBitSet getBitSet() {
		return this.ioBitSet;
	}
	
	/* return true is all pieces are available in a file */
	public boolean isComplete() {
			for (int i = 0; i < this.mask.length; i++) 
				if (this.mask[i] == false) return false;
			return true;
	}
	
	/* returns size of all pieces except the last one, which might
	 * be irregular
	 */
	public int getPieceSize() {
		return this.pieceSize;
	}
	
	/* returns irregular piece size */
	public int getLastPieceSize() {
		return this.irregPieceSize;
	}
	
	/* closes all open files for wirte */
	public void close() throws IOException {
		for (int i = 0; i < files.length; i++) 
			files[i].close();
	}
	/*
	 * @return number of bytes to download
	 */
	public long bytesRemaining() {
		long remaining = 0;
		int i;
		for (i = 0; i < mask.length - 1; i++)
			if (!mask[i]) remaining += pieceSize;
		if (!mask[i]) remaining += this.irregPieceSize;
		return remaining;
	}
	
	private class MyIOBitSet implements IOBitSet {

		public Iterator<Integer> getEmptyPiecesIterator() {
			return new EmptyPiecesIterator();
		}

		public int getNumEmptyPieces() {
			int num = 0;
			for (int i = 0; i < mask.length; i++) 
				if (mask[i] == false) num++;
			return num;
		}

		public BitSet getUnsyncBitSet() {
			BitSet s = new BitSet(mask.length);
			for (int i = 0; i < 0; i++)
				if (mask[i]) s.set(i);
			return s;
		}

		public boolean havePiece(int index)  {
			if (index < 0 || index > mask.length) 
				throw new InternalError("requested piece: " 
						+ index + " does not exists");
			return mask[index];
		}

		public int totalNumOfPieces() {
			return mask.length;
		}	
		
		private class EmptyPiecesIterator implements Iterator<Integer> {

			private int emptyIndex = 0; /* -1 indicates that there is no empty pieces */
			
			private void seekNextEmptyPiece() {
				while (emptyIndex < mask.length 
						&& mask[emptyIndex] == true) 
					emptyIndex++;
				if (emptyIndex == mask.length) emptyIndex = -1;
			}

			public boolean hasNext() {
				if (emptyIndex == -1) return false;
				return true;
			}

			public Integer next() {
				int nextEmptyPiece = emptyIndex;
				seekNextEmptyPiece();
				return nextEmptyPiece;
			}

			public void remove() {
				throw new UnsupportedOperationException("You should never try to remove anything from IO bitmask");
			}
			
		}
	}
	

	
	private void dprint(String message) {
		if (Main.DEBUG)
			System.out.println("*** IO: " + message);
	}

}
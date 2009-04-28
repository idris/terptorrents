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
	private MyIOBitSet ioBitSet = new MyIOBitSet();

	
	private IO(MetaFile m) throws IOException {
		dprint("Starting...");	
		/* create folders structure */
		prepareFolders(m);
		/* create dummy files to download, if necessary */
		createFiles(m);
		if (files.length == 0) 
			throw new IOException("File list to download is empty");
		this.pieceSize = m.getPieceLength().intValue();
		this.irregPieceSize = (int) (files[files.length - 1].length() % pieceSize);
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
	}
	
	private void prepareFolders(MetaFile m) {
		List<String> paths = m.getFileFolders();
		if (paths.isEmpty()) return;
		File f;
		dprint("Preparing folders srtucture");
		for (String path : paths) {
			f = new File(path);
			f.mkdirs();
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
		LinkedList<RandomAccessFile> filesList = new LinkedList<RandomAccessFile>();
		long length;
		for (String fileName : filenames) {
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
			filesList.add(newFile);
		}
		/* initialize initial files[] array */
		this.files = new RandomAccessFile[filesList.size()];
		for(int i = 0; i < this.files.length; i++) 
			this.files[i] = filesList.get(i);
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
		int startOffset = i*pieceSize;
		int endOffset = startOffset + pieceSize;
		RandomAccessFile startFile = findFile(startOffset);
		RandomAccessFile endFile = findFile(endOffset);		
		/* possible cases:
		 * 1. No such piece
		 * 2. Piece is entirely inside one file
		 * 3. Irregular piece
		 * 4. Piece is on the boundary of 2 files
		 */
		/* 1 */
		if (startFile == null && endFile == null) 
			throw new TerptorrentsIONoSuchPieceException("Requested piece #" + i + " does not exists");
		byte[] result;
		int numRead = 0;
		/* 2 */
		if (startFile == endFile) {
			result = new byte[pieceSize];
			startFile.seek(startOffset);
			numRead = startFile.read(result);
		/* 3 */
		} else if (startFile != null && endFile == null){
			result = new byte[irregPieceSize];
			startFile.seek(startOffset);
			numRead = startFile.read(result);
		/* 4 */
		} else {
			result = new byte[pieceSize];
			byte[] tmp = new byte[pieceSize];
			int tmpNumRead;
			numRead = 0;
			/* copy part of the piece from the file on the left */
			tmpNumRead = startFile.read(tmp, startOffset, (int) startFile.length() % pieceSize);
			System.arraycopy(tmp, 0, result, 0, tmpNumRead);
			numRead += tmpNumRead;
			/* copy part of the piece from the file on the right */
			tmpNumRead = endFile.read(tmp, 0, pieceSize - numRead);
			System.arraycopy(tmp, 0, result, numRead, tmpNumRead);
			numRead += tmpNumRead;
		}
		if (numRead < 0 || numRead != result.length)
			throw new TerptorrentsIOCodeLogicBrokenException();
		return result;
	}
	
	/* return file which have specified byte of data */
	private RandomAccessFile findFile(long offset) throws IOException {
		RandomAccessFile f = null;
		long totalLength = 0;
		for (int i = 0; i < files.length; i++) {
			totalLength += files[i].length();
			if (offset < totalLength)
				return files[i];
		}
		return f;			
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
		int startOffset = i*pieceSize;
		int endOffset = startOffset + pieceSize;
		RandomAccessFile startFile = findFile(startOffset);
		RandomAccessFile endFile = findFile(endOffset);		
		/* possible cases:
		 * 1. No such piece
		 * 2. Piece is entirely inside one file
		 * 3. Irregular piece
		 * 4. Piece is on the boundary of 2 files
		 */
		/* 1 */
		if (startFile == null && endFile == null) 
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
		if (startFile == endFile) {
			startFile.seek(startOffset);
			startFile.write(piece);
		/* 3 */
		} else if (startFile != null && endFile == null){
			dprint("Writing irregular pice #" + i);
			if (piece.length != this.irregPieceSize)
				throw new TerptorrentsIONoSuchPieceException
					("writePiece(): Irregular piece size does not match");
			startFile.seek(startOffset);
			startFile.write(piece);
		/* 4 */
		} else {
			int firstHalfOfPiece = (int) startFile.length() % pieceSize;
			int secondHalfOfPiece = pieceSize - firstHalfOfPiece;
			startFile.write(piece, startOffset, firstHalfOfPiece);
			endFile.write(piece, endOffset, secondHalfOfPiece);
		}
		mask[i] = true;
		return true;
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
		return this.getPieceSize();
	}
	
	/* returns irregular piece size */
	public int getLastPieceSize() {
		return this.getLastPieceSize();
	}
	
	public void close() throws IOException {
		for (int i = 0; i < files.length; i++) 
			files[i].close();
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
			seekNextEmptyPiece();
			if (emptyIndex == -1) return false;
			return true;
		}

		public Integer next() {
			return emptyIndex;
		}

		public void remove() {
			throw new UnsupportedOperationException("You should never try to remove anything from IO bitmask");
		}
		
	}
	
	private void dprint(String message) {
		if (Main.DEBUG)
			System.out.println("*** IO: " + message);
	}

}

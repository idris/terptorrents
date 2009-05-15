package terptorrents.io;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metainfo.MetaFile;
import metainfo.TorrentParser;

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
	private HashSet<Integer> piecesWeDoNotWant = new HashSet<Integer>();
	private HashSet<Integer> highPriorityPieces = new HashSet<Integer>();
	private HashSet<Integer> excludedFiles = new HashSet<Integer>();
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
	    if (Main.ENABLE_SELECTIVE_DOWNLOAD) selectFilesForDownload();
	    if (Main.ENABLE_FILE_PRIORITY_SELECTION) setPriority();
	}

	/* checks pieces in a file against SHA1 and mark mask[] if piece needs
	 * to be downloaded
	 */
	private void checkFilesIntegrity() throws IOException {
		dprint("Checking file(s) integrity...");
		byte[] piece;
		dprint("Marking pieces to download: (total: " + mask.length + ")");
		Main.print("Preparing file(s)");
		int numOfDots = 0;
		for (int i = 0; i < mask.length; i++) {
			try {
				piece = this.getPiece(i);
				digest.update(piece);
				byte[] hash = digest.digest();				
				if (!Arrays.equals(hash, this.pieceHashes.get(i))) {
					mask[i] = false;
				} else {
					mask[i] = true;
				}
				/* ---Print progress bar ---*/
				if (Main.DEBUG || Main.INFO) {
					if (!mask[i]) System.out.print(i + "  ");
					else System.out.print("X  ");
					numOfDots += 3;
				} else {
					System.out.print(".");
					numOfDots++;
				}
				if (numOfDots > 80) {System.out.println(); numOfDots = 0;}
				/* ------------------------- */
			} catch (TerptorrentsIONoSuchPieceException e) {
				dprint("Integrity Checking failed. Reason: " + e.getMessage());
				throw new InternalError("checkFilesIntegrity() function failed. Requested piece does not exists");
			} catch (IODeselectedPieceException e) {
				//Ignore integrity checking of deselected pieces
			}
		}
		Main.print("\n");
		if (Main.DEBUG) System.out.println("\n Number of pieces to download: " + this.getBitSet().getNumEmptyPieces());
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
	public synchronized byte[] getPiece(int i) throws IOException, 
	TerptorrentsIONoSuchPieceException, IODeselectedPieceException {
		if (DEBUG) dprint("Piece #" + i + " requested");
		if (i < 0 || i >= mask.length) 
			throw new TerptorrentsIONoSuchPieceException("Requested index:" + i + 
					" is out of bounds");
		if (piecesWeDoNotWant.contains(i))
			throw new IODeselectedPieceException(
					"Following piece bolong to a file we should ignore");
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
	private synchronized int findFile(long offset, LongContainer cont) throws IOException {
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
	
	
	/**
	 * Computes SHA1 of the piece and writes it into the file
	 * @return false if SHA1 does not match with SHA1 in MetaFile
	 */
	public synchronized boolean writePiece(int i, byte[] piece) throws IOException, TerptorrentsIONoSuchPieceException {
		if (DEBUG) dprint("Writing piece #" + i + " Size: " + piece.length);
		if (i < 0 || i >= mask.length) 
			throw new TerptorrentsIONoSuchPieceException("Requested index:" + i + 
					" is out of bounds");
		if (piece.length > this.pieceSize)
			throw new TerptorrentsIONoSuchPieceException(
					"writePiece() Given piece is > than pieceSize");

		if (piecesWeDoNotWant.contains(i)) {
			//ignore deselected pieces
			dprint("Following piece #" + i + " was deselected by user");
			return true;
		}
		/* if we downloaded this piece already, ignore it */
		if (mask[i] == true) {
			dprint("Piece #" + i + " is already written");
			return true;
		}
		/* piece lenght checking */
		if (i == mask.length - 1 && piece.length != irregPieceSize){
			dprint("Piece #" + i + " is irregular one, but its size " 
					+ piece.length + " does not match with " + irregPieceSize);
			return false;
		} else if (i != mask.length - 1 && piece.length != pieceSize) {
			dprint("Piece #" + i + " is regular piece, but its size " + 
					piece.length + " does not match with " + pieceSize);
			return false;
		}
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
if (DEBUG) dprint("Successfully wrote piece #" + i + " Size: " + piece.length);
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
				if (this.mask[i] == false && !piecesWeDoNotWant.contains(i)) return false;
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
	
	/* returns set of high priority pieces */
	public HashSet<Integer> getHighPriorityPieces() {
		return new HashSet<Integer>(this.highPriorityPieces);
	}
	
	/* closes all open files for write */
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
		for (i = 0; i < mask.length - 1; i++) {
			if (!mask[i] && !piecesWeDoNotWant.contains(i)) remaining += pieceSize;
		}
		if (!mask[i] && !piecesWeDoNotWant.contains(i)) remaining += this.irregPieceSize;
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
			for (int i = 0; i < mask.length; i++)
				if (mask[i]) s.set(i);
			return s;
		}

		public boolean havePiece(int index) throws IODeselectedPieceException  {
			if (index < 0 || index > mask.length) 
				throw new InternalError("requested piece: " 
						+ index + " does not exists");
			if (piecesWeDoNotWant.contains(index))
				throw new IODeselectedPieceException("");
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
	
	/* Interactive select of the files user don't want to download */
	private void selectFilesForDownload() {
		/* print filenames to choose from */
		List<String> files = TorrentParser.getInstance().getMetaFile().getFilenames();
		System.out.println("\nChoose files you don't want to download... ");
		/* print filename and its number STARTING FROM ONE */
		for (int i = 0; i < files.size(); i++) {
			System.out.println("" + (i+1) + ". " + files.get(i));
		}
		System.out.print("Enter file numbers separated by space: ");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			/* get user input */
			List<Integer> userInput = this.getIntegers(in.readLine());
			int lastFile = this.files.length;
			Set<Integer> filesToExclude = new HashSet<Integer>();
			for (Integer i: userInput) {
				if (i <= lastFile && i > 0) filesToExclude.add(i-1);
			}
			/* exclude pieces we do not need */
			if (!filesToExclude.isEmpty()) {
				System.out.print("\nFollowing files were removed from download list:");
				for (Integer i : filesToExclude)
					System.out.print(" " + (i + 1));
				System.out.println();
				this.excludeFiles(filesToExclude);
				this.excludedFiles.addAll(filesToExclude);
			} else System.out.println("Downloading all files");
		} catch (IOException e) {
			System.out.println("Sorry, could not read a string from you. Downloading all pieces");
		}
		dprint("Following pieces were excluded from download: " + piecesWeDoNotWant);
	}
	
	private void excludeFiles(Set<Integer> files) throws IOException {
		FileTuple ft;
		for (int i = 0 ; i < mask.length; i++) {
			/* for each piece find start and end file */
			ft = findFiles(i);
			/* if piece is entirely inside one file */
			if (ft.startFile == ft.endFile) {
				if (files.contains(ft.startFile)) piecesWeDoNotWant.add(i);
			} else if (ft.endFile != -1 && ft.startFile == ft.endFile +1) { //two consecutive files
				if (files.contains(ft.startFile) && files.contains(ft.endFile))
						piecesWeDoNotWant.add(i);
			} else if (ft.startFile != -1 && ft.endFile != -1) {
				//two non consecutive files
				boolean downloadPiece = false;
				for (int file = ft.startFile; file <= ft.endFile; file++) {
					if (!files.contains(file)) {
						//some file is inside a piece
						downloadPiece = true;
						break;
					}
				}
				if (!downloadPiece) piecesWeDoNotWant.add(i);
			} else if (ft.startFile != -1 && ft.endFile == -1) {
				//last piece
				boolean downloadPiece = false;
				for (int file = ft.startFile; file < this.files.length; file++) {
					if (!files.contains(file)) {
						//some file is inside a piece
						downloadPiece = true;
						break;
					}
				}
				if (!downloadPiece) piecesWeDoNotWant.add(i);
			}
		}
	}
	
	/* SELECT PRIORITY OF THE FILES */
	private void setPriority() {
		/* print filenames to choose from */
		List<String> files = TorrentParser.getInstance().getMetaFile().getFilenames();
		System.out.println("\nChoose files you want to give a higher priority... ");
		/* print filename and its number STARTING FROM ONE */
		for (int i = 0; i < files.size(); i++) {
			if (excludedFiles.contains(i)) {continue;}
			System.out.println("" + (i+1) + ". " + files.get(i));
		}
		System.out.print("Enter file numbers separated by space: ");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			/* get user input */
			List<Integer> userInput = this.getIntegers(in.readLine());
			int lastFile = this.files.length;
			Set<Integer> filesToPrioritize = new HashSet<Integer>();
			for (Integer i: userInput) {
				if (i <= lastFile && i > 0 
						&& !excludedFiles.contains(i-1)) filesToPrioritize.add(i-1);
			}
			/* mark higher priority pieces */
			if (!filesToPrioritize.isEmpty()) {
				System.out.print("\nFollowing files will have higher priority:");
				for (Integer i : filesToPrioritize)
					System.out.print(" " + (i + 1));
				System.out.println();
				
				/* mark high priority pieces */
				this.markHighPriorityPieces(filesToPrioritize, highPriorityPieces);

			} else System.out.println("All files will have default priority");
		} catch (IOException e) {
			System.out.println("Sorry, could not read a string from you. Files will have default priority");
		}
		dprint("Following pieces where marked as high priority pieces: " + highPriorityPieces);
	}
	
	
	/* mark high priority pieces */
	private void markHighPriorityPieces(Set<Integer> files, Set<Integer> pieces) throws IOException {
		FileTuple ft;
		for (int i = 0 ; i < mask.length; i++) {
			/* for each piece find start and end file */
			ft = findFiles(i);
			/* if piece is entirely inside one file */
			if (ft.startFile == ft.endFile) {
				if (files.contains(ft.startFile)) pieces.add(i);
			} else if (ft.endFile != -1 && ft.startFile == ft.endFile +1) { //two consecutive files
				if (files.contains(ft.startFile) || files.contains(ft.endFile))
					pieces.add(i);
			} else if (ft.startFile != -1 && ft.endFile != -1) {
				//two non consecutive files, check if any of those files 
				//have a priority
				for (int f = ft.startFile; f <= ft.endFile; f++) {
					if (files.contains(f)) {
						pieces.add(i);
						break;
					}
				}
			} else if (ft.startFile != -1 && ft.endFile == -1) {
				//last piece
				for (int file = ft.startFile; file < this.files.length; file++) {
					if (files.contains(file)) {
						pieces.add(i);
						break;
					}
				}
			}
		}
	}
	
	private class FileTuple{int startFile; int endFile;};
	private FileTuple findFiles(int piece) throws IOException {
		long startOffset = pieceSize*piece;
		long endOffset = startOffset + pieceSize;
		FileTuple ft = new FileTuple();
		ft.startFile = this.findFile(startOffset, new LongContainer());
		ft.endFile = this.findFile(endOffset, new LongContainer());
		return ft;
	}
	
	private List<Integer> getIntegers(String s) {
		List<Integer>returnValue=new ArrayList<Integer>();
		Pattern p = Pattern.compile("(\\d+)");
		Matcher m = p.matcher(s);
		while (m.find()) {
			Integer added=new Integer(s.substring(m.start(), m.end()));
			returnValue.add(added);
		}
		return returnValue;
	}
	
	public int getNumOfIgnoredPieces() {
		return piecesWeDoNotWant.size(); 
	}
	
	private void dprint(String message) {
		if (Main.DEBUG)
			System.out.println("*** IO: " + message);
	}

}

package terptorrents.io;

import java.io.FileNotFoundException;
import java.io.IOException;

import terptorrents.exceptions.IODeselectedPieceException;
import terptorrents.exceptions.TerptorrentsIONoSuchPieceException;

import metainfo.*;

public class IOTestingClass {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidBEncodingException 
	 * @throws TerptorrentsIONoSuchPieceException 
	 */
	public static void main(String[] args) throws InvalidBEncodingException, FileNotFoundException, IOException, TerptorrentsIONoSuchPieceException {;
		TorrentParser.instantiate("piratemaryland.jpg.torrent");
		MetaFile m = TorrentParser.getInstance().getMetaFile();
		IO.instantiate(m);
		for (int i = 0 ; i < IO.getInstance().getBitSet().totalNumOfPieces(); i++) {
			byte[] piece;
			try {
				piece = IO.getInstance().getPiece(i);
				IO.getInstance().writePiece(i, piece);
			} catch (IODeselectedPieceException e) {
				System.out.println("Piece #" + i + " was deselected");
			}
			
		}
		System.out.println("Num: " + IO.getInstance().getBitSet().getUnsyncBitSet().length());
	}

}

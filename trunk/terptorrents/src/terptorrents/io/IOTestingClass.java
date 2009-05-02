package terptorrents.io;

import java.io.FileNotFoundException;
import java.io.IOException;

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
		TorrentParser.instantiate("");
		MetaFile m = TorrentParser.getInstance().getMetaFile();
		IO.instantiate(m);
		IO io = IO.getInstance();
		for (int i = 0; i < 95; i++) {
			byte[] piece = io.getPiece(i);
			io.writePiece(i, piece);
		}
	}

}

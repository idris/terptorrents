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
		TorrentParser.instantiate("maryland.jpg.torrent");
		MetaFile m = TorrentParser.getInstance().getMetaFile();
		IO.instantiate(m);
		System.out.println(IO.getInstance().getBitSet().getUnsyncBitSet().length());
	}

}

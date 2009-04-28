package terptorrents.io;

import java.io.FileNotFoundException;
import java.io.IOException;

import metainfo.*;

public class IOTestingClass {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidBEncodingException 
	 */
	public static void main(String[] args) throws InvalidBEncodingException, FileNotFoundException, IOException {
		TorrentParser parser = new TorrentParser("maryland.jpg.torrent");
		parser.parse();
		MetaFile m = parser.getMetaFile();
		IO.instantiate(m);
	}

}

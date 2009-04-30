package metainfo;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TorrentFileTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TorrentParser myParser=new TorrentParser("sam.torrent");
		try {
			myParser.parse();
		} catch (InvalidBEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

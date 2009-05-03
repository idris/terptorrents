package metainfo;

import java.io.IOException;
import java.net.URLEncoder;

import com.sun.jndi.toolkit.url.UrlUtil;

import terptorrents.Main;
import terptorrents.Stats;
import terptorrents.io.IO;
import terptorrents.util.TerpURL;

public class ParserTester {

	/**
	 * @param args
	 * @throws IOException 
	 */
	/*
	  http://vip.tracker.thepiratebay.org/announce?info_hash=%fb%ff%3c%46%d7%6f%dc%d4%ff%6c%74%bb%ce%a5%7e%fd%4c%18%8c%3b
	  &peer_id=%00%00%00%00%00%00%00%00%00%03%03%03%fe%07%a3%b8%67%cd%8b%e8
	  &port=6881&uploaded=0&downloaded=0&left=251658240&event=started
	 */
	public static void main(String[] args) throws IOException {
		TorrentParser.instantiate("beyonce.torrent");
		String url = TorrentParser.getInstance().getMetaFile().getAnnounce();
		IO.instantiate(TorrentParser.getInstance().getMetaFile());
		System.out.println();
		//url = TorrentParser.getInstance().getMetaFile().computeInfoHash();
		
		url += "?info_hash=" + TorrentParser.getInstance().getMetaFile().getURLInfoHash() +
		"&peer_id=" + TerpURL.urlencode(Main.PEER_ID) +
		"&port=" + Main.PORT +
		"&uploaded=" + Stats.getInstance().downloaded +
		"&downloaded=" + Stats.getInstance().uploaded +
		"&left=" + IO.getInstance().bytesRemaining() +
		"&event=started" +
		"&compact=1";

		System.out.println("URL: " + url);
	}

}

package terptorrents;

import java.io.File;
import java.io.IOException;

import terptorrents.comm.*;
import terptorrents.io.IO;

import metainfo.*;


public class Main {
	/* ****************************************************** */
	public static boolean DEBUG = false;	
	public static final int MAX_REQUEST_BUFFER_SIZE = 1 << 27;
	public static final int NUM_PIECES_TO_EVICT = 8;
	public static final int MAX_REQUEST_BLOCK_SIZE = 1 << 12;
	public static final int OPTIMISTIC_UNCHOKE_FREQUENCY = 3;
	public static final int NUM_PEERS_TO_UNCHOKE = 3;
	/* ****************************************************** */
	
	private static String torrentFile;
	private final static String USAGE = " <-d> <.torrent>";
	/* ARGUMENTS: 
	 * -d : debugging mode
	 * last argument should be a .torrent file
	 */	
	public static void main(String[] args) {
		dprint("Starting Terptorrent...");
		parseCommand(args);
		try {
			/* Parsing .torrent file */
			dprint("Parsing .torrent file");
			TorrentParser parser = new TorrentParser(torrentFile);
			parser.parse();
			MetaFile metaFile = parser.getMetaFile();
			
			/* instantiate IO layer */
			dprint("Instantiating IO layer");
			IO.instantiate(metaFile);
			
			/* instantiate Tracker Communicator */
			dprint("Launching Tracker Communicator");
			Thread trackerComm = new Thread(new TrackerCommunicator());
			//make thread a daemon, so it dies when Main exits
			trackerComm.setDaemon(true);
			trackerComm.start();
			
			/* instantiate ConnectionPool */
			dprint("Instantiating ConnectionPool");
			ConnectionPool.newInstance();

			/* BRAIN stuff */
			//TODO
			
		} catch (IOException e) {
			dprint("IOException is caught. Reason: " + e);
		}		
	}
	
	private static void parseCommand(String[] args) {
		/* parse arguments*/
		for (String arg : args) {
			/* debugging mode */
			if (arg.equals("-d")) Main.DEBUG = true;
			/* add more arguments if needed */
		}
		/* last argument should always be a .torrent file */
		if (args.length == 0) {
			System.out.println("Usage: " + Main.USAGE);
		}
		
		torrentFile = args[args.length - 1];
		/* check if specified torrent file exists */
		File f = new File(torrentFile);
		if (!f.isFile())
			System.out.println("Specified .torrent file does not exists");		
	}
	
	private static void dprint(String message) {
		System.out.print("MAIN: " + message);
	}
	
}
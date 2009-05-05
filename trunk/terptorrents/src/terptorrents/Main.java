package terptorrents;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import terptorrents.comm.*;
import terptorrents.io.IO;
import terptorrents.models.ChockingAlgorithm;
import terptorrents.models.PieceManager;

import metainfo.*;


public class Main {
	/* ****************************************************** */
	public static boolean DEBUG = true;	
	public static final int MAX_REQUEST_BUFFER_SIZE = 1 << 28;
	public static final int NUM_PIECES_TO_EVICT = 8;
	public static final int MAX_REQUEST_BLOCK_SIZE = 1 << 14;
	public static final int OPTIMISTIC_UNCHOKE_FREQUENCY = 3;
	public static final int NUM_PEERS_TO_UNCHOKE = 3;
	public static final int CHOCKING_ALGORITHM_INTERVAL = 5000;
	public static final int MAX_PEER_CONNECTIONS = 40;
	public static byte [] PEER_ID = "-TR1340-TerpTorrents".getBytes();
	public static final int PORT = 6881;
	public static boolean ENABLE_SELECTIVE_DOWNLOAD = false;
	private static final int TIME_TO_CHECK_IF_FILE_IS_COMPLETE = 10000;
	/* ****************************************************** */
	
	
	private static String torrentFile;
	private final static String USAGE = " <-d> <.torrent>";
	/* ARGUMENTS: 
	 * -d : debugging mode
	 * last argument should be a .torrent file
	 */		
	public static void main(String[] args) {
		dprint("Starting Terptorrent...");
		//TODO remove comment. It is OFF for debuggin purpose
		torrentFile = "piratemaryland.jpg.torrent";
		//parseCommand(args);
		
		
		try {
			/* Generate Client ID */
			generatePeerID();
			
			/* Parsing .torrent file */
			dprint("Parsing .torrent file");
			TorrentParser.instantiate(torrentFile);
			MetaFile metaFile = TorrentParser.getInstance().getMetaFile();

			/* instantiate IO layer */
			dprint("Instantiating IO layer");
			IO.instantiate(metaFile);
			/* init piece manager */
			dprint("Starting Piece Manager");
			PieceManager.initialize();

			/* instantiate Tracker Communicator */
			dprint("Launching Tracker Communicator");
			Thread trackerComm = new Thread(new TrackerCommunicator());
			//make thread a daemon, so it dies when Main exits
			trackerComm.setDaemon(true);
			trackerComm.start();

			/*start connection pool*/
			dprint("Instantiating ConnectionPool");
			ConnectionPool.newInstance();
			
			
			/*start peer listener*/
			dprint("Starting peer listener");
			Thread peerListener = new Thread(new PeerListener(PORT));
			//make thread a daemon, so it dies when Main exits
			peerListener.setDaemon(true);
			peerListener.start();
			
			/*start chocking algorithm*/
			dprint("Starting choking algorithm");
			Thread chockingAlgorithm = new Thread(new ChockingAlgorithm());
			//make thread a daemon, so it dies when Main exits
			chockingAlgorithm.setDaemon(true);
			chockingAlgorithm.start();




			while(true){
				if (IO.getInstance().isComplete())
					System.out.println("***** FILE DOWNLOAD COMPLETE. Seeding. *****");
				try {
					Thread.sleep(Main.TIME_TO_CHECK_IF_FILE_IS_COMPLETE);
				} catch (InterruptedException e) {
					dprint("Exiting...");
					/* ********************************* */
					/* closing Bittorrent procedures     */
					IO.getInstance().close();
					System.exit(1);
				}
			}
			
		} catch (IOException e) {
			dprint("IOException is caught. Reason: " + e);
		}		
	}

	private static void parseCommand(String[] args) {
		/* parse arguments*/
		for (String arg : args) {
			/* debugging mode */
			if (arg.equals("-d")) Main.DEBUG = true;
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

	public static void dprint(String message) {
		System.out.println("Debug : " + message);
	}
	
	private static void generatePeerID(){
		PEER_ID = new byte[20];
		Random r = new Random(System.currentTimeMillis());
		r.nextBytes(PEER_ID);
		dprint("Client ID: " + PEER_ID);
	}

}
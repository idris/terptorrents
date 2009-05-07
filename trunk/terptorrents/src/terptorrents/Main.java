package terptorrents;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

import terptorrents.comm.*;
import terptorrents.io.IO;
import terptorrents.models.ChockingAlgorithm;
import terptorrents.models.PieceManager;
import terptorrents.models.RequestManager;

import metainfo.*;


public class Main {
	/* ****************************************************** */
	private static final String ID_PREFIX = "TerpTorrent_";
	public static byte [] PEER_ID;
	/* ------------------------------- */
	public static boolean DEBUG = true;
	public static boolean INFO = false;
	/* ------------------------------- */
	public static final int MAX_REQUEST_BUFFER_SIZE = 1 << 28;
	public static final int NUM_PIECES_TO_EVICT = 8;
	public static final int MAX_REQUEST_BLOCK_SIZE = 1 << 14;
	public static final int OPTIMISTIC_UNCHOKE_FREQUENCY = 10;
	public static final int NUM_PEERS_TO_UNCHOKE = 4;
	public static final int CHOCKING_ALGORITHM_INTERVAL = 1000;
	public static final int MAX_PEER_CONNECTIONS = 20;
	/* ------------------------------------- */
	public static boolean   USER_ASSIGNED_PORT = false; //set to true if port is read form user
	public static int 		PORT;
	public static final int MIN_PORT = 6881;
	public static final int MAX_PORT = 6889;
	/* ------------------------------------- */
	public static boolean 	ENABLE_SELECTIVE_DOWNLOAD = false;
	private static final int TIME_TO_CHECK_IF_FILE_IS_COMPLETE = 5000;
	public static final int TIME_BETWEEN_RETRANSMITION_OF_UNREPLIED_REQUEST_MESSAGES = 3000;
	public static final int NUM_OF_PIECES_LEFT_TO_TRIGGER_END_GAME = 4;
	public static final int MAX_OUTSTANDING_REQUESTS = 10;
	
	/* ****************************************************** */

	private static String torrentFile;
	private final static String USAGE = " <-d> <.torrent>";
	/* ARGUMENTS: 
	 * -d : debugging mode
	 * last argument should be a .torrent file
	 */
	public static void main(String[] args) {
		dprint("Starting Terptorrent...");
		//TODO remove comment. It is OFF for debugging purpose
		torrentFile = "Linux_Server_Edition.torrent";
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
			
			/* get port for PeerListener */
			dprint("Creating socket for PeerListener");
			ServerSocket peerListenerSocket = getSocketForIncommingConnections();
			if (peerListenerSocket == null) {
				System.out.println("All ports in from " + MIN_PORT + " to " 
						+ MAX_PORT + " are taken.");
			}
			dprint("Client is listening on port " + Main.PORT);

			/* instantiate Tracker Communicator */
			dprint("Launching Tracker Communicator");
			Thread trackerComm = new Thread(new TrackerCommunicator());
			//make thread a daemon, so it dies when Main exits
			trackerComm.setDaemon(true);
			trackerComm.start();

			/*start connection pool*/
			dprint("Instantiating ConnectionPool");
			/* start it in a thread, because it can take time to launch it
			 * if a lot of peers returned by tracker */
			Thread connectionPool = new Thread(new Runnable(){
				public void run() {
					ConnectionPool.newInstance();
				}				
			});
			connectionPool.setDaemon(true);
			connectionPool.start();

			/*start peer listener*/
			dprint("Starting peer listener");
			Thread peerListener = new Thread(new PeerListener(peerListenerSocket));
			//make thread a daemon, so it dies when Main exits
			peerListener.setDaemon(true);
			peerListener.start();
			
			/*start chocking algorithm*/
			dprint("Starting choking algorithm");
			Thread chockingAlgorithm = new Thread(new ChockingAlgorithm(), "CHOKING ALGORITHM");
			//make thread a daemon, so it dies when Main exits
			chockingAlgorithm.setDaemon(true);
			chockingAlgorithm.start();

			dprint("Starting Request Manager");
			Thread requestManager = new Thread(RequestManager.getInstance());
			requestManager.setDaemon(true);
			requestManager.start();



			boolean seeding = false;
			while(true){
				if (IO.getInstance().isComplete()) {
					System.out.println("***** FILE DOWNLOAD COMPLETE. Seeding. *****");
					if(!seeding) {
						seeding = true;
						ConnectionPool.getInstance().removeSeeders();
					}
				} else {
					System.out.println("***** REMAINING DATA TO DOWNLOAD: " + IO.getInstance().bytesRemaining()/1024 + "K *****");
				}

				try {
					Thread.sleep(Main.TIME_TO_CHECK_IF_FILE_IS_COMPLETE);
				} catch (InterruptedException e) {
					dprint("Exiting...");
					/* ********************************* */
					/* closing BitTorrent procedures     */
					IO.getInstance().close();
					System.exit(1);
				}
			}

		} catch (IOException e) {
			dprint("IOException is caught. Reason: " + e.getMessage());
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
		if(DEBUG) {
			System.out.println("Debug : " + message);
		}
	}

	public static void iprint(String message) {
		if(INFO) {
			System.out.println("Info : " + message);
		}
	}
	
	/* look for available port in range for Peer Listener */
	private static ServerSocket getSocketForIncommingConnections() {
		// 6881-6889
		ServerSocket s;
		int p;
		for (p = MIN_PORT; p <= MAX_PORT; p++) {
			try {
				Main.PORT = p;
				s = new ServerSocket(p);
				return s;
			} catch (IOException e) {
				Main.dprint("Could not assignt port #" + p + " to a Listenre. Retrying...");
			}
		}
		return null;
	}

	private static void generatePeerID(){
		PEER_ID = new byte[20];
		byte[] peerPrefix = Main.ID_PREFIX.getBytes();
		byte[] randomID = new byte[20 - peerPrefix.length];
		System.arraycopy(peerPrefix, 0, PEER_ID, 0, peerPrefix.length);
		Random r = new Random(System.currentTimeMillis());
		r.nextBytes(randomID);
		System.arraycopy(randomID, 0, PEER_ID, peerPrefix.length, randomID.length);
		String randPart = "";
		for (int i = 0 ; i < randomID.length; i++) {
			randPart += Math.abs(randomID[i]);
		}
		dprint("Client ID: " + Main.ID_PREFIX + randPart);
	}

}
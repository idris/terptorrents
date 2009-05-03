package terptorrents.comm;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import metainfo.*;

import terptorrents.Main;
import terptorrents.Stats;
import terptorrents.exceptions.TrackerResponseException;
import terptorrents.io.IO;
import terptorrents.models.Peer;
import terptorrents.models.PeerList;
import terptorrents.util.TerpURL;

public class TrackerCommunicator implements Runnable {
	private static final String EVENT_STARTED = "started";
	private static final String EVENT_STOPPED = "stopped";
	private static final String EVENT_COMPLETED = "completed";
	
	
	private final byte[] peerId;
	private final int port;
	private final String infoHash;
	private final Stats stats = Stats.getInstance();
	private final boolean compact = true;

	private String trackerId = null;
	private boolean completed = false;
	private boolean stopped = false;
	
	// Tracker response fields
	private List<Peer>peerList;
	private int interval = 1000*60*30; //time to wait between requests, in seconds
	private int minInterval; //optional, time to wait between announce
	private int numSeeders;
	private int numLeechers;
	

	public TrackerCommunicator() throws IOException {
		peerId = Main.PEER_ID;
		port = Main.PORT;
		infoHash = TorrentParser.getInstance().getMetaFile().getURLInfoHash();
		pingTracker(EVENT_STARTED);
	}

	public void run() {
		try {
			Thread.sleep(interval);
		} catch(InterruptedException ex) {
			// keep going
		}

		while(!completed) {
			try {
				pingTracker();
			} catch(IOException ex) {
				// oh well... maybe next time.
			}

			try {
				Thread.sleep(interval);
			} catch(InterruptedException ex) {
				// keep going
			}
		}

		try {
			pingTracker(EVENT_COMPLETED);
		} catch(IOException ex) {
			// oh well... maybe next time.
		}

		while(!stopped) {
			try {
				Thread.sleep(interval);
			} catch(InterruptedException ex) {
				// keep going
			}

			try {
				pingTracker();
			} catch(IOException ex) {
				// oh well... maybe next time.
			}
		}

		try {
			pingTracker(EVENT_STOPPED);
		} catch(IOException ex) {
			// oh well... maybe next time.
		}
	}

	public void stop() {
		stopped = true;
	}


	private void pingTracker() throws IOException {
		pingTracker(null);
	}
	private void pingTracker(String event) throws IOException {
		String trackerURL = TorrentParser.getInstance().getMetaFile().getAnnounce();

		trackerURL += generateQueryString(event);

		URL url = new URL(trackerURL);
		URLConnection conn = url.openConnection();
		
		
		/*InputStreamReader reader = new InputStreamReader(conn.getInputStream());
		StringBuffer buf = new StringBuffer();
		CharBuffer charBuf = CharBuffer.wrap(buf);
		while(reader.read(charBuf) > 0);*/
		
		try {
			handleResponse(conn.getInputStream());
		} catch (TrackerResponseException e) {
			e.printStackTrace();
		}
		
	}

	private String generateQueryString(String event) {
		String str = "?info_hash=" + infoHash +
		"&peer_id=" + TerpURL.urlencode(peerId) +
		"&port=" + port +
		"&uploaded=" + stats.uploaded +
		"&downloaded=" + stats.downloaded +
		"&left=" + IO.getInstance().bytesRemaining() +
		"&compact=" + (compact?"1":"0");
		
		if(event != null) {
			str += "&event=" + event;
		}
		if(trackerId != null) {
			str += "&trackerid=" + trackerId;
		}
		return str;
	}

	private void handleResponse(InputStream responseStream) throws InvalidBEncodingException, IOException, TrackerResponseException {
		BDecoder bdecoder = new BDecoder(responseStream);
		BEValue failureReasonBE;
		Map topLevelMap=bdecoder.bdecode().getMap();
		failureReasonBE=(BEValue)(topLevelMap.get("failure reason"));
		if(failureReasonBE!=null){ //presence of "failure reason" in response indicates failed tracker communication 
			throw new TrackerResponseException("Tracker Responded With Failure: \n"+failureReasonBE.getString());
		}
		else{
			BEValue intervalBE=(BEValue)(topLevelMap.get("interval"));
			if(intervalBE!=null)interval=intervalBE.getInt();
			BEValue peersBE=(BEValue)(topLevelMap.get("peers"));
			if(peersBE!=null){
				peerList=new ArrayList<Peer>();
				byte[]peerBytes=peersBE.getBytes();
				for(int i=0; i<peerBytes.length; i+=6){
					byte[]ip=new byte[4];
					for(int j=0; j<4; j++){
						ip[j]=peerBytes[i+j];
					}
					int port = 0;
					port |= peerBytes[i+4];
					port <<= 8;
					port |= peerBytes[i+5];
					String dottedIP=InetAddress.getByAddress(ip).getHostAddress();
					peerList.add(new Peer(new String(dottedIP+":"+port).getBytes(),dottedIP,port));
					PeerList.getInstance().addPeers(peerList);
				}
			}
			BEValue trackerIDBE=(BEValue)(topLevelMap.get("tracker id"));
			if(trackerIDBE!=null)trackerId=trackerIDBE.getString();
		}	
	}
}
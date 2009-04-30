package terptorrents.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.util.Map;

import metainfo.*;

import terptorrents.Stats;
import terptorrents.tracker.TrackerRequest;
import terptorrents.tracker.TrackerResponse;

public class TrackerCommunicator implements Runnable {
	private static final String EVENT_STARTED = "started";
	private static final String EVENT_STOPPED = "stopped";
	private static final String EVENT_COMPLETED = "completed";

	private long requestInterval = 1000*60*30; // thirty minutes
	private String trackerId = null;
	private final String peerId;
	private final int port;
	private final String infoHash;
	private final Stats stats = Stats.getInstance();
	private final boolean compact = true;
	private boolean completed = false;
	private boolean stopped = false;

	public TrackerCommunicator() throws IOException {
		// TODO: use real values
		peerId = "my peer id";
		port = 1234;
		infoHash = "some info hash here";
		pingTracker(EVENT_STARTED);
	}

	public void run() {
		while(!completed) {
			try {
				Thread.sleep(requestInterval);
			} catch(InterruptedException ex) {
				// keep going
			}

			try {
				pingTracker(null);
			} catch(IOException ex) {
				// oh well... maybe next time.
			}
		}

		while(!stopped) {
			try {
				pingTracker(EVENT_COMPLETED);
			} catch(IOException ex) {
				// oh well... maybe next time.
			}

			try {
				Thread.sleep(requestInterval);
			} catch(InterruptedException ex) {
				// keep going
			}
		}
	}

	public void stop() throws IOException {
		stopped = true;
		pingTracker(EVENT_STOPPED);
	}

	private void pingTracker(String event) throws IOException {
		String trackerURL = "http://blahblahblah";

		trackerURL += generateQueryString(event);

		URL url = new URL(trackerURL);
		URLConnection conn = url.openConnection();
		
		
		/*InputStreamReader reader = new InputStreamReader(conn.getInputStream());
		StringBuffer buf = new StringBuffer();
		CharBuffer charBuf = CharBuffer.wrap(buf);
		while(reader.read(charBuf) > 0);*/

		handleResponse(conn.getInputStream());
	}

	private String generateQueryString(String event) {
		int left = 555; // TODO: get number of bytes remaining from IO

		String str = "info_hash=" + infoHash +
		"peer_id" + peerId +
		"port" + port +
		"uploaded" + stats.uploaded.get() +
		"downloaded" + stats.downloaded.get() +
		"left" + left +
		"compact" + (compact ? "1" : "0") +
//		"no_peer_id" + no_peer_id +
		"event" + event;
//		"ip" + ip +
//		"numwant" + numwant +
//		"key" + key +
		if(trackerId != null) {
			str += "trackerid" + trackerId;
		}

		return str;
	}

	private void handleResponse(InputStream responseStream) throws InvalidBEncodingException, IOException {
		BDecoder bdecoder = new BDecoder(responseStream);
		Map topLevelMap=bdecoder.bdecode().getMap();
	}
}

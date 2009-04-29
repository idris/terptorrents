package terptorrents.comm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;

import terptorrents.tracker.TrackerRequest;
import terptorrents.tracker.TrackerResponse;

public class TrackerCommunicator implements Runnable {
	private static final long REQUEST_INTERVAL = 1000*60*30; // thirty minutes

	public void run() {
		try {
			pingTracker();
		} catch(IOException ex) {
			// oh well... maybe next time.
		}

		try {
			Thread.sleep(REQUEST_INTERVAL);
		} catch(InterruptedException ex) {
			// keep going
		}
	}

	public void pingTracker() throws IOException {
		String trackerURL = "http://blahblahblah";

		TrackerRequest request = new TrackerRequest();
		trackerURL += request.toQueryString();

		URL url = new URL(trackerURL);
		URLConnection conn = url.openConnection();
		InputStreamReader reader = new InputStreamReader(conn.getInputStream());
		StringBuffer buf = new StringBuffer();
		CharBuffer charBuf = CharBuffer.wrap(buf);
		while(reader.read(charBuf) > 0);

		String responseText = buf.toString();

		TrackerResponse response = new TrackerResponse(responseText);

		// do something with the response
	}
}

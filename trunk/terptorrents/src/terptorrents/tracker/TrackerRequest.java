package terptorrents.tracker;

public class TrackerRequest {
	public static final String EVENT_STARTED = "started";
	public static final String EVENT_STOPPED = "stopped";
	public static final String EVENT_COMPLETED = "completed";

	String info_hash;
	String peer_id;
	int port;
	int uploaded;
	int downloaded;
	int left;
	int compact = 1;
	String event;
	int numwant;
	String key = "terptorrents";
	String trackerid;

	public String toQueryString() {
		return "info_hash=" + info_hash +
		"peer_id" + peer_id +
		"port" + port +
		"uploaded" + uploaded +
		"downloaded" + downloaded +
		"left" + left +
		"compact" + compact +
//		"no_peer_id" + no_peer_id +
		"event" + event +
//		"ip" + ip +
		"numwant" + numwant +
		"key" + key +
		"trackerid" + trackerid;
	}
}

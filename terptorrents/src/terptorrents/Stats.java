package terptorrents;

import java.util.concurrent.atomic.AtomicInteger;

public class Stats {
	private static final Stats singleton = new Stats();

	public final AtomicInteger uploaded = new AtomicInteger();
	public final AtomicInteger downloaded = new AtomicInteger();

	public static Stats getInstance() {
		return singleton;
	}
}

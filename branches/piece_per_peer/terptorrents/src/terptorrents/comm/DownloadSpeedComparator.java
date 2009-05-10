/**
 * 
 */
package terptorrents.comm;

import java.util.Comparator;

import terptorrents.Main;

/**
 * @author jonli
 *
 */
public class DownloadSpeedComparator implements Comparator<PeerConnection> {

	public int compare(PeerConnection o1, PeerConnection o2) {
		Main.iprint("download speed - " + o1.getPeer().toString() + ": " + o1.getDownloadRate());
		Main.iprint("download speed - " + o2.getPeer().toString() + ": " + o2.getDownloadRate());
		
		if(o1.getDownloadRate() == o2.getDownloadRate())
			return 0;
		else if(o1.getDownloadRate() < o2.getDownloadRate())
			return -1;
		else
			return 1;
	}
}

/**
 * 
 */
package terptorrents.comm;

import java.util.Comparator;

/**
 * @author jonli
 *
 */
public class DownloadSpeedComparator implements Comparator<PeerConnection> {

	public int compare(PeerConnection o1, PeerConnection o2) {
		if(o1.getDownloadRate() == o2.getDownloadRate())
			return 0;
		else if(o1.getDownloadRate() < o2.getDownloadRate())
			return -1;
		else
			return 1;
	}
}

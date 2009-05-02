/**
 * 
 */
package terptorrents.comm;

import java.util.Comparator;

/**
 * @author jonli
 *
 */
public class UploadSpeedComparator implements Comparator<PeerConnection> {
	@Override
	public int compare(PeerConnection o1, PeerConnection o2) {
		if(o1.getUploadRate() == o2.getUploadRate())
			return 0;
		else if(o1.getUploadRate() < o2.getUploadRate())
			return -1;
		else
			return 1;
	}
}

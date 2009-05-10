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
public class UploadSpeedComparator implements Comparator<PeerConnection> {
	
	public int compare(PeerConnection o1, PeerConnection o2) {
		Main.iprint("uplaod speed - " + o1.getPeer().toString() + ": " + o1.getDownloadRate());
		Main.iprint("upload speed - " + o2.getPeer().toString() + ": " + o2.getDownloadRate());
		
		if(o1.getUploadRate() == o2.getUploadRate())
			return 0;
		else if(o1.getUploadRate() < o2.getUploadRate())
			return -1;
		else
			return 1;
	}
}

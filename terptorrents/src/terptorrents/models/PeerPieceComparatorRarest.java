/**
 * 
 */
package terptorrents.models;

import java.util.Comparator;

/**
 * @author Jonathan
 *
 */
public class PeerPieceComparatorRarest implements Comparator<PeerPiece> {

	@Override
	public int compare(PeerPiece arg0, PeerPiece arg1) {
		if(arg0.getNumPeer() == arg1.getNumPeer())
			return 0;
		else if (arg0.getNumPeer() < arg1.getNumPeer())
			return -1;
		else
			return 1;
	}

}

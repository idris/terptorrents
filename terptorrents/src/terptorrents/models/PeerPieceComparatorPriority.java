package terptorrents.models;

import java.util.Comparator;

public class PeerPieceComparatorPriority implements Comparator<PeerPiece> {

	public int compare(PeerPiece arg0, PeerPiece arg1) {
		if(arg0.getNumPeer() == arg1.getNumPeer())
			return 0;
		else if (arg0.getPriority() < arg1.getPriority())
			return -1;
		else
			return 1;
	}
}

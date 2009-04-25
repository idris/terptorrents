/**
 * 
 */
package terptorrents.models;

import java.util.Comparator;

/**
 * @author Jonathan
 *
 */
public class LocalPieceComparatorLRU implements Comparator<LocalPiece> {

	@Override
	public int compare(LocalPiece o1, LocalPiece o2) {
		if(o1.getNumRequest() == o2.getNumRequest())
			return 0;
		else if(o1.getNumRequest() < o2.getNumRequest())
			return -1;
		else 
			return 1;
	}

}

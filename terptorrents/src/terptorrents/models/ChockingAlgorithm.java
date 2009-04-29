/**
 * 
 */
package terptorrents.models;

import java.util.ArrayList;
import java.util.PriorityQueue;

import terptorrents.Main;
import terptorrents.comm.ConnectionPool;
import terptorrents.comm.PeerConnection;
import terptorrents.io.IO;

/**
 * @author Jonathan
 *
 */
public class ChockingAlgorithm implements Runnable {
	static int countOptimistic = 0;
	
	@Override
	public void run() {
		if(IO.getInstance().isComplete()){
			//leeching mode


				
		}else{
			//seeding mode
						
			if(countOptimistic % Main.OPTIMISTIC_UNCHOKE_FREQUENCY == 0){
				Peer PlannedOptimisticUnchokedPeer = 
					ConnectionPool.getInstance().
					getPlannedOptimisticUnchokedPeerConnection().getPeer();
				countOptimistic++;
				ArrayList<PeerConnection> UploaderList = 
					ConnectionPool.getInstance().getActiveAndInterested();
			}	
		}
		
	}

}

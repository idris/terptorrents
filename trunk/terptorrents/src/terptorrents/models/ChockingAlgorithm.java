/**
 * 
 */
package terptorrents.models;

import java.util.Vector;

import terptorrents.Main;
import terptorrents.comm.ConnectionPool;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.ChokeMessage;
import terptorrents.comm.messages.UnchokeMessage;
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
			
			Vector<PeerConnection> peersToUnchoke = 
				ConnectionPool.getInstance().getSeedableConnections();
			
			for(int i = 0 ; i < Main.NUM_PEERS_TO_UNCHOKE; i++){
				if(!ConnectionPool.getInstance().getUnchoked().
						contains(peersToUnchoke.get(i))){
					peersToUnchoke.get(i).sendMessage(new UnchokeMessage());
				}
			}
			for(PeerConnection unchokedPeer : ConnectionPool.getInstance().getUnchoked()){
				if(!peersToUnchoke.subList(0, Main.NUM_PEERS_TO_UNCHOKE).
						contains(peersToUnchoke))
					unchokedPeer.sendMessage(new ChokeMessage());
			}
			if(countOptimistic % Main.OPTIMISTIC_UNCHOKE_FREQUENCY == 0){
				if(!ConnectionPool.getInstance().getUnchoked().
						contains(peersToUnchoke.get( Main.NUM_PEERS_TO_UNCHOKE))){
					peersToUnchoke.get( Main.NUM_PEERS_TO_UNCHOKE).
					sendMessage(new UnchokeMessage());
				}
			}else{
				ConnectionPool.getInstance().
				getPlannedOptimisticUnchokedPeerConnection().
				sendMessage(new UnchokeMessage());
			}
			countOptimistic++;
		}else{
			//seeding mode
			Peer plannedOptimisticUnchokedPeer = null;
			if(countOptimistic % Main.OPTIMISTIC_UNCHOKE_FREQUENCY == 0){
				plannedOptimisticUnchokedPeer = 
					ConnectionPool.getInstance().
					getPlannedOptimisticUnchokedPeerConnection().getPeer();
			}

			Vector<PeerConnection> uploaderList = 
				ConnectionPool.getInstance().getActiveAndInterested();
			for(int i = 0; i < Main.NUM_PEERS_TO_UNCHOKE; i++){
				if(!ConnectionPool.getInstance().getUnchoked().
						contains(uploaderList.get(i))){
					uploaderList.get(i).sendMessage(new UnchokeMessage());
				}
			}
			for(PeerConnection unchokedPeer : ConnectionPool.getInstance().getUnchoked()){
				if(!uploaderList.subList(0, Main.NUM_PEERS_TO_UNCHOKE).
						contains(uploaderList))
					unchokedPeer.sendMessage(new ChokeMessage());
			}
			if(plannedOptimisticUnchokedPeer != null){
				do{
					while(uploaderList.subList(0, Main.NUM_PEERS_TO_UNCHOKE).
							contains(plannedOptimisticUnchokedPeer.getConnection()))
						plannedOptimisticUnchokedPeer = 
							ConnectionPool.getInstance().
							getPlannedOptimisticUnchokedPeerConnection().getPeer();
					
					plannedOptimisticUnchokedPeer.getConnection().
					sendMessage(new UnchokeMessage());
				}while(!ConnectionPool.getInstance().getInstersted().
						contains(plannedOptimisticUnchokedPeer));
			}
		}
		countOptimistic++;
	}	
}
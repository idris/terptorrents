/**
 * 
 */
package terptorrents.models;

import java.util.Vector;

import terptorrents.Main;
import terptorrents.comm.ConnectionPool;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.ChokeMessage;
import terptorrents.comm.messages.RequestMessage;
import terptorrents.comm.messages.UnchokeMessage;
import terptorrents.exceptions.TerptorrentsModelsCanNotRequstFromThisPeer;
import terptorrents.io.IO;

/**
 * @author Jonathan
 *
 */
public class ChockingAlgorithm implements Runnable {
	static int countOptimistic = 0;

	public void run() {
		while(true){

			/*send request message*/
			for(PeerConnection peerCon: ConnectionPool.
					getInstance().getNonChokingAndInsterested()){
				try {
					for(BlockRange br : PieceManager.getInstance().
							getBlockRangeToRequest(peerCon.getPeer())){
						peerCon.sendMessage(new RequestMessage(
								br.getPieceIndex(), br.getBegin(), 
								br.getLength()));
					}
				} catch (TerptorrentsModelsCanNotRequstFromThisPeer e) {
					e.printStackTrace();
				}
			}

			/*choking algorithm*/
			if(IO.getInstance().isComplete()){
				//seeding mode
				Vector<PeerConnection> peersToUnchoke = 
					ConnectionPool.getInstance().getSeedableConnections();
				Vector<PeerConnection> unchokedPeers = 
					ConnectionPool.getInstance().getUnchoked();

				for(int i = 0 ; i < Math.min(peersToUnchoke.size(), Main.NUM_PEERS_TO_UNCHOKE); i++){
					if(!unchokedPeers.contains(peersToUnchoke.get(i))){
						peersToUnchoke.get(i).sendMessage(new UnchokeMessage());
					}
				}
				for(PeerConnection unchokedPeer : unchokedPeers){
					if(!peersToUnchoke.subList(0, Main.NUM_PEERS_TO_UNCHOKE).
							contains(peersToUnchoke))
						unchokedPeer.sendMessage(new ChokeMessage());
				}
				/* unchoke peer based on the rank 4th one or the lat one*/
				if(countOptimistic % Main.OPTIMISTIC_UNCHOKE_FREQUENCY == 0){
					int lastPeer = peersToUnchoke.size() - 1;
					if (lastPeer > 0) {
						PeerConnection onePeerToUnchoke = peersToUnchoke.get(
								Math.min(Main.NUM_PEERS_TO_UNCHOKE, lastPeer));
						if(!unchokedPeers.contains(onePeerToUnchoke)) {
							onePeerToUnchoke.sendMessage(new UnchokeMessage());
						}
					}
				}else{
					ConnectionPool.getInstance().
					getPlannedOptimisticUnchokedPeerConnection().
					sendMessage(new UnchokeMessage());
				}
				countOptimistic++;
			}else{
				//leeching mode
				PeerConnection plannedOptimisticUnchokedPeer = null;
				if(countOptimistic % Main.OPTIMISTIC_UNCHOKE_FREQUENCY == 0){
					plannedOptimisticUnchokedPeer = 
						ConnectionPool.getInstance().
						getPlannedOptimisticUnchokedPeerConnection();
				}

				Vector<PeerConnection> uploaderList = 
					ConnectionPool.getInstance().getActiveAndInterested();
				Vector<PeerConnection> unchokedPeers = 
					ConnectionPool.getInstance().getUnchoked();


				for(int i = 0; i < Math.min(uploaderList.size(), Main.NUM_PEERS_TO_UNCHOKE); i++){
					if(!unchokedPeers.contains(uploaderList.get(i))){
						uploaderList.get(i).sendMessage(new UnchokeMessage());
					}
				}
				for(PeerConnection unchokedPeer : unchokedPeers){
					if(!uploaderList.subList(0, Math.min(uploaderList.size(), Main.NUM_PEERS_TO_UNCHOKE)).
							contains(uploaderList))
						unchokedPeer.sendMessage(new ChokeMessage());
				}
				if(plannedOptimisticUnchokedPeer != null){
					do{
						while(uploaderList.subList(0, Main.NUM_PEERS_TO_UNCHOKE).
								contains(plannedOptimisticUnchokedPeer))
							plannedOptimisticUnchokedPeer = 
								ConnectionPool.getInstance().
								getPlannedOptimisticUnchokedPeerConnection();

						plannedOptimisticUnchokedPeer.
						sendMessage(new UnchokeMessage());
					}while(!ConnectionPool.getInstance().getInstersted().
							contains(plannedOptimisticUnchokedPeer));
				}
			}
			countOptimistic++;
			try {
				Thread.sleep(Main.CHOCKING_ALGORITHM_INTERVAL);
			} catch (InterruptedException e) {
			}
		}
	}
}
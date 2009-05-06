/**
 * 
 */
package terptorrents.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import terptorrents.Main;
import terptorrents.comm.ConnectionPool;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.ChokeMessage;
import terptorrents.comm.messages.UnchokeMessage;
import terptorrents.exceptions.TerptorrentsModelsCanNotRequstFromThisPeer;
import terptorrents.io.IO;

/**
 * @author Jonathan
 *
 */
public class ChockingAlgorithm implements Runnable {
	static int countOptimistic = 0;


	/*XXX Idris Copy from here*/
	static HashSet<BlockRange> requestedBlock = new HashSet<BlockRange>();

	/**
	 * Be careful, this function can return null
	 * @param peer
	 * @param numBlockRanges
	 * @return
	 */
	static ArrayList<BlockRange> GetBlockRangeToReqeust(Peer peer, 
			int numBlockRanges){
		try {
			ArrayList<BlockRange> blockRanges = PieceManager.getInstance().
			getBlockRangeToRequest(peer, requestedBlock, numBlockRanges);
			for(BlockRange br : blockRanges)
				requestedBlock.add(br);
			return blockRanges;
		} catch (TerptorrentsModelsCanNotRequstFromThisPeer e) {
			Main.dprint("Can not request from this peer EXCEPTION IS CAUGHT: "
					+ peer.toString());
			return null;
		}
	}
	
//	public void run(){
//		while(true){
//			requestedBlock.clear();
//			try {
//				Thread.sleep(Main.
//				TIME_BETWEEN_RETRANSMITION_OF_UNREPLIED_REQUEST_MESSAGES);
//			} catch (InterruptedException e) {
//			}
//		}
//	}

	/*Idris: Stop here*/
	
	public void run() {
		/*choking algorithm*/
		if(IO.getInstance().isComplete()){
			//seeding mode
			Vector<PeerConnection> peersToUnchoke = 
				ConnectionPool.getInstance().getSeedableConnections();
			Vector<PeerConnection> unchokedPeers = 
				ConnectionPool.getInstance().getUnchoked();

			int minPeersToUnchoke = Math.min(peersToUnchoke.size(), 
					Main.NUM_PEERS_TO_UNCHOKE);
			for(int i = 0 ; i < minPeersToUnchoke; i++){
				if(!unchokedPeers.contains(peersToUnchoke.get(i))){
					peersToUnchoke.get(i).sendMessage(new UnchokeMessage());
				}
			}

			/* Sergey: boundary condition check added */
			for(PeerConnection unchokedPeer : unchokedPeers){
				int numPeersToUnchoke = peersToUnchoke.size();
				int min = Math.min(Main.NUM_PEERS_TO_UNCHOKE, numPeersToUnchoke);
				if (numPeersToUnchoke != 0
						&& !peersToUnchoke.subList(0, min).
						contains(peersToUnchoke)) {						
					unchokedPeer.sendMessage(new ChokeMessage());
				}

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
				PeerConnection pc = ConnectionPool.getInstance().
				getPlannedOptimisticUnchokedPeerConnection();
				if (pc != null) 
					pc.sendMessage(new UnchokeMessage());
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


			for(int i = 0; i < Math.min(uploaderList.size(), 
					Main.NUM_PEERS_TO_UNCHOKE); i++){
				if(!unchokedPeers.contains(uploaderList.get(i))){
					uploaderList.get(i).sendMessage(new UnchokeMessage());
				}
			}
			for(PeerConnection unchokedPeer : unchokedPeers){
				if(!uploaderList.subList(0, Math.min(uploaderList.size(), 
						Main.NUM_PEERS_TO_UNCHOKE)).
						contains(uploaderList))
					unchokedPeer.sendMessage(new ChokeMessage());
			}
			if(plannedOptimisticUnchokedPeer != null){
				do{
					// Sergey. Boundary checking 
					int min = Math.min(uploaderList.size(), Main.NUM_PEERS_TO_UNCHOKE);
					if (min == 0) break; //no one to unchoke
					while(uploaderList.subList(0, min)
							.contains(plannedOptimisticUnchokedPeer))
						plannedOptimisticUnchokedPeer = 
							ConnectionPool.getInstance()
							.getPlannedOptimisticUnchokedPeerConnection();

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
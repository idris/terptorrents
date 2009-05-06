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

	
	//TODO empty RequestedBlock only after certain time period 
	public void run() {
		HashSet<BlockRange> RequestedBlock = new HashSet<BlockRange>();
		HashSet<BlockRange> copyOfRequestedBlocks = new HashSet<BlockRange>();
		long timeStamp = System.currentTimeMillis();
		long curTimeSnapShot;
		int numOfRetries = 0;
		Vector<PeerConnection> peersWeSendRequestsTo = new Vector<PeerConnection>();
		Vector<PeerConnection> copyOfpeersWeSendRequestsTo = new Vector<PeerConnection>();
		ClientMode clientMode;
		
		while(true){
			/* detect our client mode */
			if (IO.getInstance().isComplete())
				clientMode = ClientMode.SEEDING;
			else clientMode = ClientMode.LEECHING;

			/* SEND REQUESTS IN LEECHING MODE */
			if (clientMode == ClientMode.LEECHING) {
				/*
				 * begin. Sergey
				 * reset RequestedBlock set only after some period
				 * of time to avoid constant retransmission of requests to the
				 * same peers
				 */
				curTimeSnapShot = System.currentTimeMillis();
				if (curTimeSnapShot - timeStamp > Main.TIME_BETWEEN_RETRANSMITION_OF_UNREPLIED_REQUEST_MESSAGES) {
					/*
					 * rotate peers if no piece message received after certain
					 * number of duplicate request of some block to the same
					 * peers
					 */
					if (copyOfRequestedBlocks.equals(RequestedBlock)
							&& !copyOfRequestedBlocks.isEmpty()) {
						numOfRetries++;
						/*
						 * if we did not hear reply from the same peers after
						 * some number of retries. Give up.
						 */
						if (numOfRetries == Main.MAX_NUM_RETRANSMISSIONs_OF_REQUEST_MESSAGES) {
							Main.dprint("Same set of request have been sent "
									+ numOfRetries + " times");
							if (copyOfpeersWeSendRequestsTo
									.equals(peersWeSendRequestsTo)
									&& !copyOfpeersWeSendRequestsTo.isEmpty()) {
								Main.dprint("Rotating peers...");
								for (PeerConnection pc : peersWeSendRequestsTo) {
									// TODO set interested or it is better to
									// Disconnect?
									//pc.setInterested(false);
									//pc.setChoked(true);
									ConnectionPool.getInstance().removeConnection(pc);
									Main.dprint("No blocks received from " + pc.toString() 
											+ " after " + numOfRetries 
											+ " retries. DISCONNECTING");
								}
							} else
								copyOfpeersWeSendRequestsTo = peersWeSendRequestsTo;
							numOfRetries = 0;
						}
					} else
						copyOfRequestedBlocks = RequestedBlock;
					RequestedBlock = new HashSet<BlockRange>();
					timeStamp = curTimeSnapShot;
				}
				/* end. Sergey */

				/* send request message */
				peersWeSendRequestsTo = ConnectionPool.getInstance()
						.getNonChokingAndInsterested();
				/* Sergey
				 * Changed for-loop so we remove peer immediately if we
				 * can not request from this peer. Otherwise loop will cause
				 * multilple request to such peers
				 */
				PeerConnection peerCon;
				for (int i = 0 ; i < peersWeSendRequestsTo.size() ; i++) {
					peerCon = peersWeSendRequestsTo.get(i);
					try {
						ArrayList<BlockRange> blockRanges = PieceManager
								.getInstance().getBlockRangeToRequest(
										peerCon.getPeer(), RequestedBlock);
						for (BlockRange br : blockRanges) {
							RequestedBlock.add(br);
							peerCon.sendMessage(new RequestMessage(br
									.getPieceIndex(), br.getBegin(), br
									.getLength()));
						}
					} catch (TerptorrentsModelsCanNotRequstFromThisPeer e) {
						if (Main.DEBUG)
							//TODO We have to do something about it
							// Remove peer? set unInterested
							System.err
									.println("Can not request from this peer EXCEPTION IS CAUGHT: "
											+ peerCon.getPeer().toString());
						Main.dprint("Removing peer from request list");
						peersWeSendRequestsTo.remove(peerCon);
					}
				}
			}

			/*choking algorithm*/
			if(clientMode == ClientMode.SEEDING){
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
	
	private enum ClientMode {LEECHING, SEEDING}
}
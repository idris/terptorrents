/**
 * 
 */
package terptorrents.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import terptorrents.comm.ConnectionPool;
import terptorrents.comm.PeerConnection;
import terptorrents.comm.messages.PieceMessage;
import terptorrents.comm.messages.RequestMessage;
import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotReadable;
import terptorrents.exceptions.TerptorrentsModelsPieceNotWritable;

/**
 * @author Jonathan
 *
 */
public class ReqeustManager implements Runnable {

	@Override
	public void run() {
		ArrayList<PeerConnection> canPeerRequestList = 
			ConnectionPool.getInstance().getUnchokedAndInterested();
		for(PeerConnection peerConnection: canPeerRequestList){
			Stack<RequestMessage> incomingRequstList = peerConnection.getIncomingRequests();
			for(RequestMessage requestMessage: incomingRequstList){
				try {
					byte data [] = PieceManager.getInstance().requestBlock(
							requestMessage.getIndex(), requestMessage.getBegin(), 
							requestMessage.getBlockLength());
					peerConnection.getOutgoingPieces().push(new PieceMessage(
							requestMessage.getIndex(), 
							requestMessage.getBegin(), data));
				} catch (TerptorrentsModelsPieceNotReadable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TerptorrentsModelsBlockIndexOutOfBound e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TerptorrentsModelsPieceIndexOutOfBound e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		}
		
		/*This doesn't seem necessary, now just used to do error checking*/
		ArrayList<PeerConnection> canIRequestList = 
			ConnectionPool.getInstance().getUnchokedAndInterested();
		
		BlockRange [] outGoingReqeusts = PieceManager.getInstance().
		getBlockRangeToRequest();
		int pieceIndex = outGoingReqeusts[0].getPieceIndex();
		try {
			HashSet<Peer> peerConnectionSet = PieceManager.
			getInstance().GetPeerSet(pieceIndex);
			int i = 0;
			for(Peer seeder: peerConnectionSet){
				assert canIRequestList.contains(seeder);
				if(i < outGoingReqeusts.length){
					seeder.getConnection().getOutgoingRequests().
					push(new RequestMessage(outGoingReqeusts[i].getPieceIndex(),
							outGoingReqeusts[i].getBegin(),
							outGoingReqeusts[i].getLength()));
					i++;
				}
			}
			
		} catch (TerptorrentsModelsPieceNotWritable e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TerptorrentsModelsPieceIndexOutOfBound e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}

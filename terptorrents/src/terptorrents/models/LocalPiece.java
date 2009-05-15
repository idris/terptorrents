/**
 * 
 */
package terptorrents.models;

import java.io.IOException;
import terptorrents.Main;
import terptorrents.exceptions.IODeselectedPieceException;
import terptorrents.exceptions.TerptorrentsIONoSuchPieceException;
import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotReadable;
import terptorrents.exceptions.TerptorrentsModelsPieceNotWritable;
import terptorrents.io.IO;

/**
 * @author jonli
 *
 */
public class LocalPiece extends Piece {
	private byte [] data;
	private int numRequest;
	
	public LocalPiece(boolean isLastPiece, int index){
		super(isLastPiece, index);
		numRequest = 0;
		data = null;
	}
	
	public int getNumRequest(){
		return numRequest;
	}
	
	public void resetNumRequest(){
		numRequest = 0;
	}
	
	public boolean isBufferInRAM(){
		return !(data == null);
	}
	
	@Override
	public synchronized byte [] requestBlock(int pieceIndex, int blockBegin, 
			int blockLength) throws TerptorrentsModelsBlockIndexOutOfBound,
			TerptorrentsModelsPieceNotReadable{
//		Main.dprint("LocalPiece: Requesting message #" + pieceIndex);
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new TerptorrentsModelsBlockIndexOutOfBound();
		if(data == null){
			try {
				data = new byte[getSize()];
				System.arraycopy(IO.getInstance().getPiece(pieceIndex), 0, 
						data, 0, getSize());
			} catch (IOException e) {
				if(terptorrents.Main.DEBUG)
					e.printStackTrace();
			} catch (TerptorrentsIONoSuchPieceException e) {
				if(terptorrents.Main.DEBUG)
					e.printStackTrace();
			} catch (IODeselectedPieceException e) {
				throw new TerptorrentsModelsPieceNotReadable();
			}
		}
		numRequest++;
		byte [] res = new byte[blockLength];
		
		System.arraycopy(data, blockBegin, res, 0, blockLength);
		
		return res;
	}

	@Override
	public boolean updateBlock(int pieceIndex,
			int begin, int length, byte [] data) 
	throws TerptorrentsModelsPieceNotWritable {
		throw new TerptorrentsModelsPieceNotWritable("Cannot update LocalPiece! {" + pieceIndex + "}");
	}

	public void clearBuffer() {
		data = null;
	}
}

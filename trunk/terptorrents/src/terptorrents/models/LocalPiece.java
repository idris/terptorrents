/**
 * 
 */
package terptorrents.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import terptorrents.exceptions.TerptorrentsIONoSuchPieceException;
import terptorrents.exceptions.TerptorrentsModelsBlockIndexOutOfBound;
import terptorrents.exceptions.TerptorrentsModelsPieceNotWritable;
import terptorrents.io.IO;

/**
 * @author jonli
 *
 */
public class LocalPiece extends Piece {
	private ByteArrayInputStream data;
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
	
	public void clearBuffer(){
		try {
			data.close();
		} catch (IOException e) {
			if(terptorrents.Main.DEBUG)
				e.printStackTrace();
		}
		data = null;
	}
	
	@Override
	public byte [] requestBlock(int pieceIndex, int blockBegin, 
			int blockLength) throws TerptorrentsModelsBlockIndexOutOfBound{
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new TerptorrentsModelsBlockIndexOutOfBound();
		if(data == null){
			try {
				data = new ByteArrayInputStream(IO.getInstance().getPiece(pieceIndex));
			} catch (IOException e) {
				if(terptorrents.Main.DEBUG)
					e.printStackTrace();
			} catch (TerptorrentsIONoSuchPieceException e) {
				if(terptorrents.Main.DEBUG)
					e.printStackTrace();
			}
		}
		numRequest++;
		byte [] res = new byte[blockLength];
		this.data.read(res, blockBegin, blockLength);
		return res;
	}

	@Override
	public boolean updateBlock(int pieceIndex,
			int begin, int length, byte [] data) 
	throws TerptorrentsModelsPieceNotWritable {
		throw new TerptorrentsModelsPieceNotWritable();
	}
}

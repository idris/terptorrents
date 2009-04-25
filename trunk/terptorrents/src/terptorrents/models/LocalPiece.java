/**
 * 
 */
package terptorrents.models;

import java.io.ByteArrayInputStream;

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
		data = null;
	}
	
	@Override
	public byte [] requestBlock(IO io, int pieceIndex, int blockBegin, 
			int blockLength) throws TerptorrentsModelsBlockIndexOutOfBound{
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new TerptorrentsModelsBlockIndexOutOfBound();
		if(data == null){
			data = new ByteArrayInputStream(io.getPiece(pieceIndex));
		}
		numRequest++;
		byte [] res = new byte[blockLength];
		this.data.read(res, blockBegin, blockLength);
		return res;
	}

	@Override
	public boolean updateBlock(IO io, int pieceIndex,
			int begin, int length, byte [] data) throws TerptorrentsModelsPieceNotWritable {
		throw new TerptorrentsModelsPieceNotWritable();
	}
}

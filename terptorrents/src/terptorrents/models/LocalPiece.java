/**
 * 
 */
package terptorrents.models;

import java.io.ByteArrayInputStream;

import terptorrents.exceptions.BlockIndexOutOfBound;
import terptorrents.exceptions.PieceNotWritable;
import terptorrents.io.IO;

/**
 * @author jonli
 *
 */
public class LocalPiece extends Piece {
	
	private ByteArrayInputStream data;
	
	public LocalPiece(boolean isLastPiece){
		super(isLastPiece);
		data = null;
	}
	
	@Override
	public byte [] requestBlock(IO io, int pieceIndex, int blockBegin, 
			int blockLength) throws BlockIndexOutOfBound{
		if(blockBegin < 0 || blockBegin + blockLength > getSize())
			throw new BlockIndexOutOfBound();
		if(data == null){
			data = new ByteArrayInputStream(io.getPiece(pieceIndex));
		}
		byte [] res = new byte[blockLength];
		this.data.read(res, blockBegin, blockLength);
		return res;
	}

	@Override
	public boolean updateBlock(IO io, int pieceIndex,
			int begin, int length, byte [] data) throws PieceNotWritable {
		throw new PieceNotWritable();
	}
}

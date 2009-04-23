/**
 * 
 */
package terptorrents.models;

import java.io.ByteArrayInputStream;

import terptorrents.exceptions.PieceNotWritable;

/**
 * @author jonli
 *
 */
public class LocalPiece extends Piece {
	
	private ByteArrayInputStream data;
	
	public LocalPiece(boolean isLastPiece){
		super(isLastPiece);
		/*TODO initialize input stream and read data from disk*/
	}

	public byte [] requestBlock(int begin, int length){
		byte [] res = new byte[length];
		this.data.read(res, begin, length);
		return res;
	}

	@Override
	public void update_piece(int begin, int length, byte [] data)
	throws PieceNotWritable {
		throw new PieceNotWritable();
	}
}

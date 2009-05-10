package terptorrents.util;

import java.util.BitSet;

import terptorrents.io.IO;

public final class BitSetUtil {
	public static BitSet fromByteArray(byte[] bytes) {
		BitSet bits = new BitSet();
		int length = IO.getInstance().getBitSet().totalNumOfPieces();
		
		for (int i = 0; i < length; i++) {
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i%8))) > 0) {
				bits.set(i);
			}
		}
		return bits;
	}

	public static byte[] toByteArray(BitSet bits, int numBytes) {
		byte[] bytes = new byte[numBytes];
		for (int i=0; i<bits.length(); i++) {
			if (bits.get(i)) {
				bytes[bytes.length-i/8-1] |= 1 << (i%8);
			}
		}
		return bytes;
	}
}

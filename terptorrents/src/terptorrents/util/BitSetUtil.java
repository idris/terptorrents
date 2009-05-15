package terptorrents.util;

import java.util.BitSet;

import terptorrents.io.IO;

public final class BitSetUtil {
	public static BitSet fromByteArray(byte[] bytes) {
		BitSet bits = new BitSet();
		int length = IO.getInstance().getBitSet().totalNumOfPieces();

		for (int i = 0; i < length; i++) {
			if ((bytes[i / 8] & (0x01 << (8 - i%8))) > 0) {
				bits.set(i);
			}
		}
//		print(bits);
//		print(bytes);
		return bits;
	}

	public static byte[] toByteArray(BitSet bits, int numBytes) {
		byte[] bytes = new byte[numBytes];
		for (int i=0; i<bits.length(); i++) {
			if (bits.get(i)) {
				bytes[i/8] |= 0x01 << (8 - i%8);
			}
		}
//		print(bits);
//		print(bytes);
		return bytes;
	}

	public static void print(byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			System.out.print(Integer.toHexString(bytes[i]));
		}
		System.out.println();
	}


	public static void print(BitSet bits) {
		for (int i=0; i<bits.length(); i++) {
			if (bits.get(i)) {
				System.out.print(1);
			} else {
				System.out.print(0);
			}
		}
		System.out.println();
		
		//		return bytes;
	}
}

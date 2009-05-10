package terptorrents.util;

public class TerpURL {
	
	/* Assumes incoming data is all raw, no ASCII */
	public static String urlEncodeBinary(byte[] rawBytes){
		int currentByteExtended,newLength=3 * rawBytes.length;
		StringBuffer returnValue = new StringBuffer(newLength);
		
		/* Replaces all bytes with a % sign followed by their Hex Value */
		for (int i = 0; i < rawBytes.length; i++){
			currentByteExtended = rawBytes[i];
			currentByteExtended &= 0xFF;
			if(currentByteExtended>=16) returnValue.append("%");
			/* Need to add a zero in front of single digit hex values */
			else returnValue.append("%0");
			returnValue.append(Integer.toHexString(currentByteExtended)); //Assuming toHexString works
		}
		return new String(returnValue);
	}
}

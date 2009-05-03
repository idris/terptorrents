package terptorrents.util;

public class TerpURL {
	public static String urlencode(byte[] bs)
	{
		StringBuffer sb = new StringBuffer(bs.length*3);
		for (int i = 0; i < bs.length; i++)
		{
			int c = bs[i] & 0xFF;
			sb.append('%');
			if (c < 16)
				sb.append('0');
			sb.append(Integer.toHexString(c));
		}

		return sb.toString();
	}
}

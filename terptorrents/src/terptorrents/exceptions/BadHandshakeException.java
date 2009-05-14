package terptorrents.exceptions;

import java.io.IOException;

@SuppressWarnings("serial")
public class BadHandshakeException extends IOException {
	public BadHandshakeException(String msg) {
		super(msg);
	}
}

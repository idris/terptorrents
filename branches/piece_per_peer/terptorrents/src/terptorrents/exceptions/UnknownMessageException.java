package terptorrents.exceptions;

@SuppressWarnings("serial")
public class UnknownMessageException extends Exception {
	String msg;

	public UnknownMessageException(String message) {
		this.msg = message;
	}

	@Override
	public String getMessage() {
		return msg;
	}
}

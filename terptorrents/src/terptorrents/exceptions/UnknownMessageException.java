package terptorrents.exceptions;

@SuppressWarnings("serial")
public class UnknownMessageException extends Exception {
	public UnknownMessageException(String message) {
		super(message);
	}
}

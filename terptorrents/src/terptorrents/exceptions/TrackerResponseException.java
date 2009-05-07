package terptorrents.exceptions;

import java.io.IOException;

public class TrackerResponseException extends IOException {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -3694086802621752062L;

	public TrackerResponseException(String message){
	    super(message);
	}
}

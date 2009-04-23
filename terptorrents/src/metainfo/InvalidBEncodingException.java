package metainfo;

import java.io.IOException;

/**
 * Exception thrown when a bencoded stream is corrupted.
 *
 * @author Sohrab Khan
 * @version 1.0
 */
public class InvalidBEncodingException extends IOException
{
  public InvalidBEncodingException(String message)
  {
    super(message);
  }
}

/* This is source code for BEncode from http://www.bitdls.com/Dissertation/BitDls-Server/ */
package metainfo;

import java.io.UnsupportedEncodingException;

import java.util.List;
import java.util.Map;

/**
 * Holds different types that a bencoded byte array can represent.
 * You need to call the correct get method to get the correct java
 * type object. If the BEValue wasn't actually of the requested type
 * you will get a InvalidBEncodingException.
 *
 * @author Sohrab Khan
 * @version 1.0
 * 
 */
public class BEValue
{
  // This is either a byte[], Number, List or Map.
  private final Object value;

  public BEValue(byte[] value)
  {
    this.value = value;
  }

  public BEValue(Number value)
  {
    this.value = value;
  }

  public BEValue(List value)
  {
    this.value = value;
  }

  public BEValue(Map value)
  {
    this.value = value;
  }

  /**
   * Returns this BEValue as a String. This operation only succeeds
   * when the BEValue is a byte[], otherwise it will throw a
   * InvalidBEncodingException. The byte[] will be interpreted as
   * UTF-8 encoded characters.
   */
  public String getString() throws InvalidBEncodingException
  {
    try
      {
	return new String(getBytes(), "UTF-8");
      }
    catch (ClassCastException cce)
      {
	throw new InvalidBEncodingException(cce.toString());
      }
    catch (UnsupportedEncodingException uee)
      {
	throw new InternalError(uee.toString());
      }
  }

  /**
   * Returns this BEValue as a byte[]. This operation only succeeds
   * when the BEValue is actually a byte[], otherwise it will throw a
   * InvalidBEncodingException.
   */
  public byte[] getBytes() throws InvalidBEncodingException
  {
    try
      {
	return (byte[])value;
      }
    catch (ClassCastException cce)
      {
	throw new InvalidBEncodingException(cce.toString());
      }
  }

  /**
   * Returns this BEValue as a Number. This operation only succeeds
   * when the BEValue is actually a Number, otherwise it will throw a
   * InvalidBEncodingException.
   */
  public Number getNumber() throws InvalidBEncodingException
  {
    try
      {
	return (Number)value;
      }
    catch (ClassCastException cce)
      {
	throw new InvalidBEncodingException(cce.toString());
      }
  }

  /**
   * Returns this BEValue as int. This operation only succeeds when
   * the BEValue is actually a Number, otherwise it will throw a
   * InvalidBEncodingException. The returned int is the result of
   * <code>Number.intValue()</code>.
   */
  public int getInt() throws InvalidBEncodingException
  {
    return getNumber().intValue();
  }

  /**
   * Returns this BEValue as long. This operation only succeeds when
   * the BEValue is actually a Number, otherwise it will throw a
   * InvalidBEncodingException. The returned long is the result of
   * <code>Number.longValue()</code>.
   */
  public long getLong() throws InvalidBEncodingException
  {
    return getNumber().longValue();
  }

  /**
   * Returns this BEValue as a List of BEValues. This operation only
   * succeeds when the BEValue is actually a List, otherwise it will
   * throw a InvalidBEncodingException.
   */
  public List getList() throws InvalidBEncodingException
  {
    try
      {
	return (List)value;
      }
    catch (ClassCastException cce)
      {
	throw new InvalidBEncodingException(cce.toString());
      }
  }

  /**
   * Returns this BEValue as a Map of BEValue keys and BEValue
   * values. This operation only succeeds when the BEValue is actually
   * a Map, otherwise it will throw a InvalidBEncodingException.
   */
  public Map getMap() throws InvalidBEncodingException
  {
    try
      {
	return (Map)value;
      }
    catch (ClassCastException cce)
      {
	throw new InvalidBEncodingException(cce.toString());
      }
  }

  public String toString()
  {
    String valueString;
    if (value instanceof byte[])
      {
	byte[] bs = (byte[])value;
	// XXX - Stupid heuristic...
	if (bs.length <= 12)
	  valueString = new String(bs);
	else
	  valueString = "bytes:" + bs.length;
      }
    else
      valueString = value.toString();

    return "BEValue[" + valueString + "]";
  }
}

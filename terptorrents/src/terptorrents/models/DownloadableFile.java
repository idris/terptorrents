package terptorrents.models;

public class DownloadableFile {
	/**
	 * Path to this file. In single file mode, this is just the filename
	 */
	private String path;

	/**
	 * Length of the file in bytes (integer)
	 */
	private Long length;

	/**
	 * (optional) a 32-character hexadecimal string corresponding to the MD5 sum of the file. This is not used by BitTorrent at all, but it is included by some programs for greater compatibility.
	 */
	private String md5sum;
	
	
	
	
}

package terptorrents.models;

import java.util.List;

public class MultiFileTorrent extends Torrent {
	/**
	 * the filename of the directory in which to store all the files. This is purely advisory. (string)
	 */
	String name;

	List<DownloadableFile> files;
}

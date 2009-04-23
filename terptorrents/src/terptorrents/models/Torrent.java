package terptorrents.models;

import java.util.Date;
import java.util.List;

public abstract class Torrent {
	String announce;
	List<String> announceList;
	Date creationDate;
	String comment;
	String createdBy;
	String encoding;

	Long pieceLength;
	List<Piece> pieces;
}

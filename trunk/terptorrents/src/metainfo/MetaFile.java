package metainfo;

import java.util.Date;
import java.util.List;
import java.util.Map;

import terptorrents.models.Piece;

public class MetaFile {

	private String announce;
	// private List<String> announceList;
	private Date creationDate;
	private String comment;
	private String createdBy;
	// private String encoding;
	private Long pieceLength;
	private List<Piece> pieces;
	private List<String> filenames;
	private Map<String, Long> fileLengths;
	private Map<Integer, byte[]> SHAHashes;

	public MetaFile(String announce, Date creationDate, String comment,
			String createdBy, Long pieceLength, List<String> filenames,
			Map<String, Long> fileLengths, Map<Integer, byte[]> hashes) {

		this.announce = announce;
		this.creationDate = creationDate;
		this.comment = comment;
		this.createdBy = createdBy;
		this.pieceLength = pieceLength;
		this.filenames = filenames;
		this.fileLengths = fileLengths;
		SHAHashes = hashes;
	}

	public String getAnnounce() {
		return announce;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public String getComment() {
		return comment;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public Long getPieceLength() {
		return pieceLength;
	}

	public List<Piece> getPieces() {
		return pieces;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	/*
	 * This will be a LinkedHashMap under the hood, so insertion order is
	 * preserved by the iterator
	 */
	public Map<String, Long> getFileLengths() {
		return fileLengths;
	}

	/* Returns a map from piece numbers to SHA Hash values */
	public Map<Integer, byte[]> getSHAHashes() {
		return SHAHashes;
	}

}

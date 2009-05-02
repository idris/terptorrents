package metainfo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private Set<String> filePaths;
	private Map<String, Long> fileLengths;
	private Map<Integer, byte[]> SHAHashes;
	private String urlInfoHash;
	private String infoHash;

	public MetaFile(String announce, Date creationDate, String comment,
			String createdBy, Long pieceLength, Set<String>filePaths,List<String> filenames,
			Map<String, Long> fileLengths, Map<Integer, byte[]> pieceHashes, String infoHash, String urlInfoHash) {

		this.announce = announce;
		this.creationDate = creationDate;
		this.comment = comment;
		this.createdBy = createdBy;
		this.pieceLength = pieceLength;
		this.filenames = filenames;
		this.fileLengths = fileLengths;
		this.filePaths=filePaths;
		SHAHashes = pieceHashes;
		this.urlInfoHash=urlInfoHash;
		this.infoHash=infoHash;
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
	 * List of path strings, not including file names
	 *  IE ["a/b/c", "c/d/e",...]
	 */
	public Set<String> getFileFolders(){
		return filePaths;
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
	
	public String getInfoHashURLEncoded(){
		return infoHash;
	}
	
	public String getURLInfoHash(){
		return urlInfoHash;
	}
	
	public String getInfoHash(){
		return infoHash;
	}

}

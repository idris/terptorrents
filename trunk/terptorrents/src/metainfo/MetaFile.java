package metainfo;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import terptorrents.models.Piece;
import terptorrents.util.TerpURL;

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
			Map<String, Long> fileLengths, Map<Integer, byte[]> pieceHashes) {

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
		return TerpURL.urlencode(this.getByteInfoHash());
	}
	
	public byte[] getByteInfoHash(){
		byte[] infoBytes = BEncoder.bencode(rebuildInfo());

		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA");
			return digest.digest(infoBytes);
		}
		catch(NoSuchAlgorithmException nsa)
		{
			throw new InternalError(nsa.toString());
		}
	}


	private Map<String, Object> rebuildInfo() {
		Map<String, Object> rebuiltInfo = new HashMap<String, Object>();
		rebuiltInfo.put("name", TorrentParser.getInstance().getName());
		rebuiltInfo.put("piece length", new Integer(pieceLength.intValue()));
		rebuiltInfo.put("pieces", TorrentParser.getInstance().getAllHashes());	
		if(filenames.size()>1){
			List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
			for (int i = 0; i < this.filenames.size(); i++){
				Map<String, Object> file = new HashMap<String, Object>();
				file.put("path", TorrentParser.getInstance().getPathLists().get(i));
				file.put("length", this.fileLengths.get(this.filenames.get(i)) );
				fileList.add(file);
			}
			rebuiltInfo.put("files", fileList);
		}
		else if(filenames.size()==1){
			rebuiltInfo.put("length", new Long(this.fileLengths.get(this.filenames.get(0))));
		}
		return rebuiltInfo;
	}

}

package metainfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import metainfo.*;


public class TorrentParser {
	
	private static TorrentParser instance = null;

	private FileInputStream stream;
	private Map topLevelMap;
	private MetaFile torrent; //only instantiated after calling parse()
	private MessageDigest digest;
	private String name;
	private byte[] allHashes;
	private List pathLists;
	
	
	public byte[] getAllHashes() {
		return allHashes;
	}

	private TorrentParser(String filename) throws IOException{
		try {
			stream=new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			throw new IOException("Parser: File Not Found");
		}
		
		try {
			digest = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("Parser: problem instantiating MessageDigest");
		}
		pathLists=new ArrayList<List<String>>();
		parse();
		
	}
	
	public static void instantiate(String fileName) throws IOException {
		if (instance == null) instance = new TorrentParser(fileName);
	}
	
	public static TorrentParser getInstance() {
		return instance;
	}
	
	public void parse() throws InvalidBEncodingException,IOException, FileNotFoundException{
		BDecoder bdecoder=null;
		bdecoder=new BDecoder(stream);
		topLevelMap=bdecoder.bdecode().getMap();
		BEValue announceBE,commentBE,createdByBE,creationDateBE,filesBE;
		String announce,comment,createdBy;
		List files;
		Date creationDate;
		
		
		/* pull simple string fields out of top level bencoded dictionary */
		announceBE=(BEValue)topLevelMap.get("announce");
		commentBE=(BEValue)topLevelMap.get("comment");
		createdByBE=(BEValue)topLevelMap.get("created by");
		creationDateBE=((BEValue)topLevelMap.get("creation date"));
		if(announceBE!=null) announce = announceBE.getString();
		else announce=null;
		if(commentBE!=null)comment = commentBE.getString();
		else comment=null;
		if(createdByBE!=null)createdBy=createdByBE.getString();
		else createdBy=null;
		if(creationDateBE!=null)creationDate=new Date(creationDateBE.getLong());
		else creationDate=null;
		
		/* pull simple fields out of info dictionary */
		BEValue infoBE=((BEValue)topLevelMap.get("info"));
		Map infoDictionary = infoBE.getMap();
		Long pieceLength = ((BEValue)infoDictionary.get("piece length")).getLong();
		filesBE=(BEValue)(infoDictionary.get("files"));
		if(filesBE!=null) files = filesBE.getList();
		else files=null;
		this.name = ((BEValue)(infoDictionary.get("name"))).getString(); //can be filename or directory name
		
		/* get concatenated SHA hash values from info dictionary */
		allHashes=((BEValue)(infoDictionary.get("pieces"))).getBytes();
		
		
		Map<Integer,byte[]> pieceHashMap;
		//String infoHash=computeInfoHash(infoBE);
		//String urlInfoHash=URLEncoder.encode(infoHash,"UTF-8");
		
		if(files==null){ // single file case
			Long lastPieceLength=0L;
			Long fileLength=((BEValue)(infoDictionary.get("length"))).getLong();
			Long numPieces=fileLength/pieceLength;
			lastPieceLength=fileLength % pieceLength;
			if(lastPieceLength != 0) numPieces++; //account for irregular last piece
			pieceHashMap=getPieceHashes(allHashes, numPieces);
			
			/* create trivial file name list,file length map, and file path list for single file case */
			List<String>singleFileList=new ArrayList<String>();
			singleFileList.add(name);
			Map<String,Long>singleFileLengthMap=new LinkedHashMap<String,Long>();
			singleFileLengthMap.put(name, fileLength);
			HashSet<String>singleFilePathSet=new HashSet<String>();
			
			// instantiate MetaFile
			torrent = new MetaFile(announce, creationDate, comment,
					createdBy, pieceLength, singleFilePathSet,singleFileList,
					singleFileLengthMap,pieceHashMap);
			
		}
		
		
		//TODO: multi-file case
		else{
			List fileList=((BEValue)(infoDictionary.get("files"))).getList();

			List<String>fileNames=getMultiFileNames(fileList);
			Set<String>filePaths=getMultiFilePaths(fileList);
			Long totalLength=0L;
			Map<String,Long>fileLengthMap=new HashMap<String,Long>();
			for(int i=0; i<fileNames.size();i++){
				Long fileLength=((BEValue)(((BEValue)fileList.get(i)).getMap().get("length"))).getLong();
				fileLengthMap.put(fileNames.get(i), fileLength);
				totalLength+=fileLength;
			}
			Long lastPieceLength=0L;
			Long numPieces=totalLength/pieceLength;
			lastPieceLength=totalLength % pieceLength;
			if(lastPieceLength != 0) numPieces++; //account for irregular last piece
			pieceHashMap=getPieceHashes(allHashes, numPieces);
			torrent = new MetaFile(announce, creationDate, comment,
					createdBy, pieceLength, filePaths,fileNames,
					fileLengthMap,pieceHashMap);
		}
		
	}
	
	//TODO: multi-file case
	private Map<String,Long> getMultiFileLengths(Map infoHash){
		return null;
	}
	
	/* Returns a list of single strings as full paths with filenames, IE  "sam/jon/idris/sergey.jpg"  */
	private List<String> getMultiFileNames(List fileMaps) throws InvalidBEncodingException{
		List<String> filePaths = new ArrayList<String>();
		for(Object fileMap : fileMaps){
			List pathList=((BEValue)(((BEValue)fileMap).getMap().get("path"))).getList();
			ArrayList<String> paths = new ArrayList<String>();
			String addedPath="";
			for(Object pathListElt : pathList){
				String newString=((BEValue)pathListElt).getString();
				paths.add(newString);
				addedPath=addedPath.concat(newString+"/");
			}
			this.pathLists.add(paths);
			if(addedPath!="")addedPath=addedPath.substring(0, addedPath.length()-1);
			filePaths.add(addedPath);
			System.out.println(addedPath);
		}
		return filePaths;
	}
	
	private Set<String> getMultiFilePaths(List fileMaps) throws InvalidBEncodingException{
		Set<String> filePaths = new HashSet<String>();
		for(Object fileMap : fileMaps){
			List pathList=((BEValue)(((BEValue)fileMap).getMap().get("path"))).getList();
			String addedPath="";
			int pathListIndex=0;
			for(Object pathListElt : pathList){
				if(!(pathListIndex==pathList.size()-1)){
					String newString=((BEValue)pathListElt).getString();
					addedPath=addedPath.concat(newString+"/");
				}
				pathListIndex++;
			}
			System.out.println("addedPath"+addedPath);
			//addedPath=addedPath.substring(0, addedPath.length()-1);
			filePaths.add(addedPath);
		}
		return filePaths;
	}
	
	/*
	public Map<String,Long> processFileList(List fileList) throws InvalidBEncodingException{
		LinkedHashMap<String,Long> returnValue=new LinkedHashMap<String,Long>();
		for(Object fileObject : fileList){
			Map fileMap = ((BEValue)fileObject).getMap();
			List<String> path=((BEValue)fileMap.get("path")).getList();
			Long length=((BEValue)fileMap.get("length")).getLong();
		}
		return null;
		
	}*/
	
	/* 
	 * Takes a byte array consisting of concatenation of all SHA-hashes and separates it into
	 * 20 byte chunks, mapping each piece number to a 20 byte hash.
	 */
	private Map<Integer,byte[]> getPieceHashes(byte[] allHashes, Long numPieces){
		Map<Integer,byte[]>returnValue=new HashMap<Integer,byte[]>();
		for(int i=0; i<numPieces; i++){
			byte[] currentHash=new byte[20];
			for(int j=0; j<20; j++) currentHash[j]=allHashes[20*i+j];
			returnValue.put(i,currentHash);
		}
		return returnValue;
	}
	
	
	public List getPathLists(){
		return pathLists;
	}
	
	public MetaFile getMetaFile(){
		return torrent;
	}
	
	public String getName() {
		return this.name;
	}
	
	
	
	
}

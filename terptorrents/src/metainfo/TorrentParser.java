package metainfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;

import metainfo.*;


public class TorrentParser {

	private FileInputStream stream;
	private String filename;
	private Map topLevelMap;
	private MetaFile torrent; //only instantiated after calling parse()
	private long totalFileLength;
	
	
	public TorrentParser(String filename){
		this.filename=filename;
		try {
			stream=new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void parse() throws InvalidBEncodingException,IOException, FileNotFoundException{
		BDecoder bdecoder=null;
		bdecoder=new BDecoder(stream);
		topLevelMap=bdecoder.bdecode().getMap();
		
		/* pull simple string fields out of top level bencoded dictionary */
		String announce = ((BEValue)topLevelMap.get("announce")).getString();
		String comment = ((BEValue)topLevelMap.get("comment")).getString();
		String createdBy=((BEValue)topLevelMap.get("createdBy")).getString();
		Date creationDate=new Date(((BEValue)topLevelMap.get("creationDate")).getLong());
		
		/* pull simple fields out of info dictionary */
		Map infoDictionary = ((BEValue)topLevelMap.get("info")).getMap();
		Long pieceLength = ((BEValue)infoDictionary.get("piece length")).getLong();
		List files = ((BEValue)(infoDictionary.get("files"))).getList();
		String name = ((BEValue)(infoDictionary.get("name"))).getString(); //can be filename or directory name
		
		/* get concatenated SHA hash values from info dictionary */
		byte[] allHashes=((BEValue)(infoDictionary.get("pieces"))).getBytes();
		Map<Integer,byte[]> pieceHashMap;
		
		if(files==null){ // single file case
			Long lastPieceLength=0L;
			Long fileLength=((BEValue)(infoDictionary.get("length"))).getLong();
			Long numPieces=fileLength/pieceLength;
			lastPieceLength=fileLength % pieceLength;
			if(lastPieceLength != 0) numPieces++; //account for irregular last piece
			pieceHashMap=getPieceHashes(allHashes, numPieces);
			
			/* create trivial file name list and file length map for single file case */
			List<String>singleFileList=new ArrayList<String>();
			singleFileList.add(name);
			Map<String,Long>singleFileLengthMap=new LinkedHashMap<String,Long>();
			singleFileLengthMap.put(name, fileLength);
			
			// instantiate MetaFile
			torrent = new MetaFile(announce, creationDate, comment,
					createdBy, pieceLength, singleFileList,
					singleFileLengthMap,pieceHashMap);
		}
		
		
		//TODO: multi-file case
		else{
			List<String>filePaths=getMultiFilePaths(((BEValue)(infoDictionary.get("files"))).getList());
			for(String path: filePaths){
				System.out.println(path);
			}
		}
		
	}
	
	//TODO: multi-file case
	private Map<String,Long> getMultiFileLengths(Map infoHash){
		return null;
	}
	
	/* Returns a list of single strings as full paths, IE  "sam/jon/idris/sergey.jpg"  */
	private List<String> getMultiFilePaths(List fileMaps) throws InvalidBEncodingException{
		List<String> filePaths = new ArrayList<String>();
		for(Object fileMap : fileMaps){
			List pathList=((BEValue)(((BEValue)fileMap).getMap().get("path"))).getList();
			String addedPath="";
			for(Object pathListElt : pathList){
				String newString=((BEValue)pathListElt).getString();
				addedPath.concat(newString+"/");
			}
			addedPath=addedPath.substring(0, addedPath.length()-1);
			filePaths.add(addedPath);
		}
		return filePaths;
	}
	
	//TODO: multi-file case
	public Map<String,Long> processFileList(List fileList) throws InvalidBEncodingException{
		LinkedHashMap<String,Long> returnValue=new LinkedHashMap<String,Long>();
		for(Object fileObject : fileList){
			Map fileMap = ((BEValue)fileObject).getMap();
			List<String> path=((BEValue)fileMap.get("path")).getList();
			Long length=((BEValue)fileMap.get("length")).getLong();
		}
		return null;
		
	}
	
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
	
	
	
	public MetaFile getMetaFile(){
		return torrent;
	}
	
	
	
	
	
	
}

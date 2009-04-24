package metainfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.security.MessageDigest;

import metainfo.*;


public class TorrentParser {

	private FileInputStream stream;
	private String filename;
	private Map topLevelMap;
	
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
		bdecoder=new BDecoder(new FileInputStream(filename));
		topLevelMap=bdecoder.bdecode().getMap();
		
		/* pull simple string fields out of top level bencoded dictionary */
		String announce = ((BEValue)topLevelMap.get("announce")).getString();
		String comment = ((BEValue)topLevelMap.get("comment")).getString();
		String createdBy=((BEValue)topLevelMap.get("createdBy")).getString();
		Long creationDate=((BEValue)topLevelMap.get("creationDate")).getLong();
		
		/* parse out fields from info dictionary */
		Map infoDictionary = ((BEValue)topLevelMap.get("info")).getMap();
		boolean multiFileMode=false;
		if(infoDictionary.get("files")!=null)multiFileMode=true;
		Long pieceLength = ((BEValue)infoDictionary.get("piece length")).getLong();
		String piecesString=((BEValue)infoDictionary.get("pieces")).getString();
		
		
		
	}
	
	public Map<String,Long> getMultiFileInfo(Map infoHash){
		
		//List<String> infoHash.get("files");
		
		return null;
		
	}
	
	
	
	
	
	
}

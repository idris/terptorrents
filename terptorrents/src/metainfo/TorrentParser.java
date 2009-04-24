package metainfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

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
	
	public void parseTorrent() throws InvalidBEncodingException,IOException, FileNotFoundException{
		BDecoder bdecoder=null;
		bdecoder=new BDecoder(new FileInputStream(filename));
		
		
		topLevelMap=bdecoder.bdecode().getMap();
		
	}
	
	
	
	
	
	
}

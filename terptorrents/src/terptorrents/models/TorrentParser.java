package terptorrents.models;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import metainfo.*;


public class TorrentParser {

	private FileInputStream stream;
	private String filename;
	private BDecoder bdecoder;
	
	public TorrentParser(String filename){
		this.filename=filename;
		try {
			stream=new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void parseTorrent(){
		try {
			bdecoder=new BDecoder(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}			
	}
	
	public void getInfoHash(){
		
	}
	
	
	
}

/**
 * Created on 
 */
package metainfo;

import com.bitdls.crawler.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import com.bitdls.torrent.*;
import com.bitdls.tracker.*;

/**
 * This class is used to Extract Information from a .torrent file.
 * 
 * @author Sohrab Khan
 * @version 1.0
 */
public class MetaInfoExtractor {
    
    private MetaInfo meta;
    private TorrentSize size;
    private ArrayList list;
    private com.bitdls.crawler.TorrentFile torrent;
    
    public MetaInfoExtractor() {
        torrent = new com.bitdls.crawler.TorrentFile();
    }
    
    public com.bitdls.crawler.TorrentFile extractInfo(String url) throws IOException{
        URL u = new URL(url);
        InputStream in = u.openStream();
        meta = new MetaInfo(new BDecoder(in));
        
        torrent.setLink(url); //link
        torrent.setComments(meta.getComments()); //comments
        torrent.setTracker(meta.getAnnounce()); //announce
        torrent.setName(meta.getName()); //name
        //get the date
        Date date = new Date(meta.getCreationDate());
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        torrent.setCreationDate(dateFormat.format(date));
        //////////////////////////////////////////////////
        
        torrent.setInfoHash(meta.getHexInfoHash()); //hashcode
        //get size
        long actual = meta.getTotalLength();
        size = new TorrentSize(actual); 
        torrent.setSize(size.getActualSize());
        //////////////////////////////////////////////////
        //file size details
        ArrayList sizes = (ArrayList) meta.getLengths();
        ArrayList files = (ArrayList) meta.getFiles();
        ArrayList fileSizes = new ArrayList();
        ArrayList fileList = new ArrayList();
        ArrayList announceList = new ArrayList();
        
        if(files!=null && sizes !=null){
            for(int i = 0; i< sizes.size(); i++){
                long size = Long.parseLong(sizes.get(i).toString());
                TorrentSize ts = new TorrentSize(size);
                fileList.add(files.get(i).toString());
                fileSizes.add(ts.getActualSize());
            }
        
            torrent.setFiles(fileList);
            torrent.setSizes(fileSizes);
        }
        ///////////////////////////////////////////
        //announce-list
        list = meta.getAnnounceList();
        
        
        if(list!= null){
            for(int i = 0; i< list.size(); i++){
                BEValue bevalue = (BEValue)list.get(i);
                ArrayList announce = (ArrayList) bevalue.getList();
                for(int j = 0; j< announce.size(); j++){
                    BEValue value = (BEValue) announce.get(j);
                    String announceValue = value.getString();
                    //
                    Announce ann = new Announce(announceValue,0,0,0);
                    if(announceList.indexOf(ann)==-1){
                        announceList.add(new Announce(value.getString(),0,0,0));
                    }
                }
            }
        }
        torrent.setAnnounceList(announceList);
        ///////////////////////////////////////////////
        
        return torrent;
    }
}

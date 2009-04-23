/*
 * TorrentSize.java
 *
 * Created on 22 October 2007, 17:29
 *
 */

package metainfo;

import java.text.*;

/**
 * This class is used to convert the size of the data represented by a torrent
 * into appropriate unit.
 * 
 * @author Sohrab Khan
 * @version 1.0
 */
public class TorrentSize {
    
    private double size;
    
    /** Creates a new instance of TorrentSize */
    public TorrentSize(long size) {
        this.size = size;
    }
    
    /**
     * This method is used to get the user friendly size i.e. in bytes, Kb, MB, GB or TB
     * @return
     */
    public String getActualSize(){
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);
        
        //if actual = 9999 bytes
        if(size > 0 && size <= 999){
            return size + "Bytes";
        }
        
        //if actual = 99,999 or 9999,999
        if(size > 999 && size <= 999999){
            size = size/1024;   
            return nf.format(size) + "KB";
        }
        
        if(size > 999999 && size <= 999999999){
            size = size/1024;
            size = size/1024;
            return nf.format(size) + "MB";
        }
        //CDec c = new CDec();
        
        if(size > 999999999L && size <= 999999999999L){
            size = size/1024;
            size = size/1024;
            size = size/1024;
            return nf.format(size) + "GB";
        }
        
        if(size > 999999999999L && size <=9999999999999999L){
            size = size/1024;
            size = size/1024;
            size = size/1024;
            size = size/1024;
            return nf.format(size) + "TB";
        }
        //actual.
        return "Invalid Size";
    }
    
}

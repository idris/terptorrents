package metainfo;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * This class is used by the MetaInfoExtractor to extract different attributes
 * from the torrent file.
 * 
 * @author Sohrab Khan
 * @version 1.0
 */
public class MetaInfo {
    private final String announce;
    private ArrayList announceList;
    private final byte[] info_hash;
    private final String name;
    private final List files;
    private final List lengths;
    private final int piece_length;
    private final byte[] piece_hashes;
    private final long length;
    private String comments;
    private long creationDate;
    //private String name;
    private byte[] torrentdata;
    
    MetaInfo(String announce, String name, List files, List lengths,
            int piece_length, byte[] piece_hashes, long length) {
        this.announce = announce;
        this.name = name;
        this.files = files;
        this.lengths = lengths;
        this.piece_length = piece_length;
        this.piece_hashes = piece_hashes;
        this.length = length;
        this.info_hash = calculateInfoHash();
    }
    
    /**
     * Creates a new MetaInfo from the given InputStream.  The
     * InputStream must start with a correctly bencoded dictonary
     * describing the torrent.
     */
    public MetaInfo(InputStream in) throws IOException {
        this(new BDecoder(in));
    }
    
    /**
     * Creates a new MetaInfo from the given BDecoder.  The BDecoder
     * must have a complete dictionary describing the torrent.
     */
    public MetaInfo(BDecoder be) throws IOException {
        // Note that evaluation order matters here...
        this(be.bdecodeMap().getMap());
    }
    
    /**
     * Creates a new MetaInfo from a Map of BEValues and the SHA1 over
     * the original bencoded info dictonary (this is a hack, we could
     * reconstruct the bencoded stream and recalculate the hash). Will
     * throw a InvalidBEncodingException if the given map does not
     * contain a valid announce string or info dictonary.
     */
    public MetaInfo(Map m) throws InvalidBEncodingException {
        BEValue val = (BEValue)m.get("announce");
        if (val == null){
            throw new InvalidBEncodingException("Missing announce string");
        } else{
            this.announce = val.getString();
        }
        
        val = (BEValue)m.get("creation date");
        if(val == null){
            System.out.println("No creation date");
        } else{
            this.creationDate = val.getLong();
        }
        
        val = (BEValue)m.get("comment");
        if(val == null){
            System.out.println("No comments");
        } else{
            this.comments = val.getString();
        }
        
        val = (BEValue)m.get("announce-list");
        if(val == null){
            System.out.println("No announce list");
        } else{
            this.announceList = (ArrayList) val.getList();
        }
        
        val = (BEValue)m.get("info");
        if (val == null)
            throw new InvalidBEncodingException("Missing info map");
        Map info = val.getMap();
        
        
        val = (BEValue)info.get("name");
        if (val == null){
            throw new InvalidBEncodingException("Missing name string");
        } else{
            name = val.getString();
        }
        
        val = (BEValue)info.get("piece length");
        if (val == null){
            throw new InvalidBEncodingException("Missing piece length number");
        } else{
            piece_length = val.getInt();
        }
        
        val = (BEValue)info.get("pieces");
        if (val == null){
            throw new InvalidBEncodingException("Missing piece bytes");
        } else{
            piece_hashes = val.getBytes();
        }
        
        val = (BEValue)info.get("length");
        if (val != null){
            // Single file case.
            length = val.getLong();
            files = null;
            lengths = null;
        } else {
            // Multi file case.
            val = (BEValue)info.get("files");
            if (val == null)
                throw new InvalidBEncodingException
                        ("Missing length number and/or files list");
            
            List list = val.getList();
            int size = list.size();
            if (size == 0)
                throw new InvalidBEncodingException("zero size files list");
            
            files = new ArrayList(size);
            lengths = new ArrayList(size);
            long l = 0;
            for (int i = 0; i < list.size(); i++) {
                Map desc = ((BEValue)list.get(i)).getMap();
                val = (BEValue)desc.get("length");
                if (val == null)
                    throw new InvalidBEncodingException("Missing length number");
                long len = val.getLong();
                lengths.add(new Long(len));
                l += len;
                
                val = (BEValue)desc.get("path");
                if (val == null)
                    throw new InvalidBEncodingException("Missing path list");
                List path_list = val.getList();
                int path_length = path_list.size();
                if (path_length == 0)
                    throw new InvalidBEncodingException("zero size file path list");
                
                List file = new ArrayList(path_length);
                Iterator it = path_list.iterator();
                while (it.hasNext())
                    file.add(((BEValue)it.next()).getString());
                
                files.add(file);
            }
            length = l;
        }
        
        info_hash = calculateInfoHash();
    }
    
    /**
     *Returns the Announce-List
     */
    public ArrayList getAnnounceList(){
        return announceList;
    }
    
    /**
     *Returns the comments
     */
    public String getComments(){
        return comments;
    }
    
    public long getCreationDate(){
        return creationDate;
    }
    
    /**
     * Returns the string representing the URL of the tracker for this torrent.
     */
    public String getAnnounce() {
        return announce;
    }
    
    /**
     * Returns the original 20 byte SHA1 hash over the bencoded info map.
     */
    public byte[] getInfoHash() {
        // XXX - Should we return a clone, just to be sure?
        return info_hash;
    }
    
    /**
     * Returns the requested name for the file or toplevel directory.
     * If it is a toplevel directory name getFiles() will return a
     * non-null List of file name hierarchy name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns a list of lists of file name hierarchies or null if it is
     * a single name. It has the same size as the list returned by
     * getLengths().
     */
    public List getFiles() {
        // XXX - Immutable?
        return files;
    }
    
    /**
     * Returns a list of Longs indication the size of the individual
     * files, or null if it is a single file. It has the same size as
     * the list returned by getFiles().
     */
    public List getLengths() {
        // XXX - Immutable?
        return lengths;
    }
    
    /**
     * Returns the number of pieces.
     */
    public int getPieces() {
        return piece_hashes.length/20;
    }
    
    /**
     * Return the length of a piece. All pieces are of equal length
     * except for the last one (<code>getPieces()-1</code>).
     *
     * @exception IndexOutOfBoundsException when piece is equal to or
     * greater then the number of pieces in the torrent.
     */
    public int getPieceLength(int piece) {
        int pieces = getPieces();
        if (piece >= 0 && piece < pieces -1)
            return piece_length;
        else if (piece == pieces -1)
            return (int)(length - piece * piece_length);
        else
            throw new IndexOutOfBoundsException("no piece: " + piece);
    }
    
    /**
     * Checks that the given piece has the same SHA1 hash as the given
     * byte array. Returns random results or IndexOutOfBoundsExceptions
     * when the piece number is unknown.
     */
    public boolean checkPiece(int piece, byte[] bs, int off, int length) {
        // Check digest
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("No SHA digest available: " + nsae);
        }
        
        sha1.update(bs, off, length);
        byte[] hash = sha1.digest();
        for (int i = 0; i < 20; i++)
            if (hash[i] != piece_hashes[20 * piece + i])
                return false;
        return true;
    }
    
    /**
     * Returns the total length of the torrent in bytes.
     */
    public long getTotalLength() {
        return length;
    }
    
    public String toString() {
        return "MetaInfo[info_hash='" + hexencode(info_hash)
        + "', announce='" + announce
                + "', name='" + name
                + "', files=" + files
                + ", #pieces='" + piece_hashes.length/20
                + "', piece_length='" + piece_length
                + "', length='" + length
                + "']";
    }
    
    /**
     * Encode a byte array as a hex encoded string.
     */
    private static String hexencode(byte[] bs) {
        StringBuffer sb = new StringBuffer(bs.length*2);
        for (int i = 0; i < bs.length; i++) {
            int c = bs[i] & 0xFF;
            if (c < 16)
                sb.append('0');
            sb.append(Integer.toHexString(c));
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a copy of this MetaInfo that shares everything except the
     * announce URL.
     */
    public MetaInfo reannounce(String announce) {
        return new MetaInfo(announce, name, files,
                lengths, piece_length,
                piece_hashes, length);
    }
    
    public byte[] getTorrentData() {
        if (torrentdata == null) {
            Map m = new HashMap();
            m.put("announce", announce);
            Map info = createInfoMap();
            m.put("info", info);
            torrentdata = BEncoder.bencode(m);
        }
        return torrentdata;
    }
    
    /**
     * Return A <code>String</code> Hexadecimal representation of the Meta Info Hash
     * 
     *@return A <code>String</code> of Hexa format (0-9 - & - A-F)
     */
    public String getHexInfoHash(){
        String text = "";
        
        if(info_hash==null){
            return null;
        } else{
            for(int i=0; i< info_hash.length; i++){
                text = text + convertDecimalToHex("" + info_hash[i]);
            }
        }
        return text;
    }
    
    /**
     * Convert a String representing a Decimal Base8 to Hexadecimal Base16.
     *
     *@return A <code>String</code> Hexa Base16
     */
    private String convertDecimalToHex(String text) {
        BigInteger num = new BigInteger(text);
        
        // If the number is negative
        if ( num.compareTo(BigInteger.ZERO) <= -1 ) {
            
            // Get the positive decimal
            num = num.multiply( (BigInteger.ONE).negate() );
            BigInteger binary = new BigInteger(num.toString(2), 2);
            
            // Invert the bits
            String mask = "1" + (binary.toString(2)).replaceAll("0", "1");
            binary = binary.xor(new BigInteger( mask, 2 ));
            
            // Add one to get a negative two's complement
            String result = binary.add( new BigInteger("1", 2 ) ).toString(2);
            
            
            // Sign extend to a hex boundary
            switch (result.length() % 4) {
                case 1:
                    result = "111" + result;
                    break;
                case 2:
                    result = "11" + result;
                    break;
                case 3:
                    result = "1" + result;
                    break;
            }
            
            // Return a hex result
            return (new BigInteger(result, 2).toString(16)).toUpperCase();
        }
        
        if ( (new BigInteger(text).toString(2)).length() % 4 == 0 ) {
            // Add a sign bit and return
            return "0" + (new BigInteger(text).toString(16)).toUpperCase();
        }
        
        // No need for a sign bit, return
        return (new BigInteger(text).toString(16)).toUpperCase();
    }
    
    /**
     * 
     * @return
     */
    private Map createInfoMap() {
        Map info = new HashMap();
        info.put("name", name);
        info.put("piece length", new Integer(piece_length));
        info.put("pieces", piece_hashes);
        if (files == null)
            info.put("length", new Long(length));
        else {
            List l = new ArrayList();
            for (int i = 0; i < files.size(); i++) {
                Map file = new HashMap();
                file.put("path", files.get(i));
                file.put("length", lengths.get(i));
                l.add(file);
            }
            info.put("files", l);
        }
        return info;
    }
    
    private byte[] calculateInfoHash() {
        Map info = createInfoMap();
        byte[] infoBytes = BEncoder.bencode(info);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            return digest.digest(infoBytes);
        } catch(NoSuchAlgorithmException nsa) {
            throw new InternalError(nsa.toString());
        }
    }
    
    
}

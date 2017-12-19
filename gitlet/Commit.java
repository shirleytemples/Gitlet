package gitlet;
import java.util.Date;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;

/** This is the class that defines what a Commit is.
 * @author Max Miranda */
public class Commit implements Serializable {
    /** A commit is initialized with a message M, a HashMap
     * of files F, a String array of parents P, and a boolean
     * C. */
    public Commit(String m, HashMap f, String[] p, boolean c) {
        _message = m;
        _files = f;
        _parents = p;
        Date dateObj;
        if (c) {
            dateObj = new Date();
            _timestamp = DATE_FORMAT.format(dateObj) + " -0800";
        } else {
            _timestamp = "Wed Dec 31 16:00:00 1969 -0800";
        }
        _universalID = hashCommit();
    }

    /** This function will hash the current commit based off
     * of the commit message, files, timestamp, and parents.
     * To return a hash. */
    public String hashCommit() {
        String files;
        if (_files == null) {
            files = "";
        } else {
            files = _files.toString();
        }
        String parents = Arrays.toString(_parents);
        return Utils.sha1(_message, files, _timestamp, parents);
    }

    /** Returns one to get the commit message of this
     * particular commit. */
    public String getMessage() {
        return _message;
    }

    /** Returns one to create an initial commit easily.*/
    public static Commit initialCommit() {
        return new Commit("initial commit", null, null, false);
    }

    /** Returns one to get all of the files that belong to a
     * particular commit. */
    public HashMap<String, String> getFiles() {
        return _files;
    }

    /** Returns one to get the timestamp of this particular
     * commit.  */
    public String getTimestamp() {
        return _timestamp;
    }

    /** Returns you to get the ID of the first parent
     * of this particular commit. */
    public String getParentID() {
        if (_parents != null) {
            return _parents[0];
        }
        return null;
    }

    /** Returns you to get the whole parents set from
     * this particular commit. */
    public String[] getParents() {
        return _parents;
    }

    /** Returns you to get the unviersal ID from the
     * particular commit. */
    public String getUniversalID() {
        return _universalID;
    }

    /** The commit message.*/
    private String _message;

    /** The date of the commit.*/
    private String _timestamp;

    /** A list of strings of hashes of Blobs that are being
     * tracked.*/
    private HashMap<String, String> _files;

    /** An array of Hashes of parents. */
    private String[] _parents;

    /** The hash of this commit. */
    private String _universalID;

    /** The date format. */
    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
}

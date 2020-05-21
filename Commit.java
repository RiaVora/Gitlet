package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** Commit Class is used to represent the contents of
 * one commit in the gitlet repository.
 * @author Ria Vora*/
public class Commit implements Serializable {

    /** Stores the String timestamp for the commit.*/
    private String _timestamp;

    /** Stores the String message for the commit.*/
    private String _message;

    /** Stores the mapping of files and SHA-1 IDS of
     * file's contents added to this commit.*/
    private HashMap<String, String> _fileToID;

    /** The commit for this ID based off of timestamp,
     * file contents, and message.*/
    private String _ID;

    /** Stores the String ID of the parent commit.*/
    private String _parent;


    /** Constructor that creates a blank commit with
     * initialized fields.*/
    public Commit() {
        _fileToID = new HashMap<String, String>();
        _ID = createID();
        _parent = "";
        _message = "";
        _timestamp = "";
    }

    /** Constructor used to create the first commit,
     * which is initialized to a certain date and time.
     * @param firstCommit is a boolean on whether
     * the user wants the first commit*/
    public Commit(boolean firstCommit) {
        this();
        if (firstCommit) {
            _timestamp = String.format("%1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                    new Date(0));
            _ID = Utils.sha1(_timestamp);
            _message = "initial commit";
        }
    }

    /** Instance method to create a timestamp for the commit,
     * called when the commit is being commited.*/
    public void createTimestamp() {
        Date currentDate = new Date(System.currentTimeMillis());
        _timestamp = String.format("%1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                currentDate);
        _ID = createID();
    }

    /** Uses the timestamp, message, and list of sorted file contents
     * in the form of SHA-1 IDS to make a unique SHA-1 ID for this commit.
     * @return the unique SHA-1 ID*/
    private String createID() {
        List<String> idsList = new ArrayList<String>(_fileToID.values());
        Collections.sort(idsList);
        idsList.add(_timestamp);
        idsList.add(_message);
        return Utils.sha1(idsList.toString());
    }

    /** Getter method for the unique SHA-1 ID for this commit.
     * @return unique SHA-1 ID*/
    public String getID() {
        return _ID;
    }

    /** Getter method for the message of this commit.
     * @return message of this commit*/
    public String getMessage() {
        return _message;
    }

    /** Uses the hashmap to return the SHA-ID of the
     * contents of the given file from this commit.
     * @param file is the canonical file path
     * @return ID of the contents*/
    public String getIDFromFile(String file) {
        if (!_fileToID.containsKey(file)) {
            return "";
        }
        return _fileToID.get(file);
    }

    /** Uses the hashmap to return the file canonical
     * pathway based off of the given SHA-ID of the
     * file's contents.
     * @param id is SHA-1 ID
     * @return file canonical pathway*/
    public String getFileFromID(String id) {
        for (String filePath: _fileToID.keySet()) {
            if (_fileToID.get(filePath).equals(id)) {
                return filePath;
            }
        }
        return null;
    }

    /** Getter method, return the hashmap of the file pathways
     * to SHA-1 IDS.
     * @return commit's hashmap*/
    public HashMap<String, String> getFileToID() {
        return _fileToID;
    }


    /** Getter method, return the ID of the parent
     * commit for this commit.
     * @return ID of parent commit*/
    public String getParent() {
        return _parent;
    }

    /** Getter method, returns the timestamp of this commit.
     * @return String version of timestamp*/
    public String getTimestamp() {
        return _timestamp;
    }

    /** Adds the given file and its contents to the
     * commit's hashmap.
     * @param f is the file to be added*/
    public void addFile(File f) throws IOException {
        String id = Utils.sha1(Utils.readContents(f));
        _fileToID.put(f.getCanonicalPath(), id);
        _ID = createID();
    }

    /** Adds the given file to be removed as part of this
     * commit.
     * @param f is the file to be removed*/
    public void addRemoveFile(File f) throws IOException {
        _fileToID.put(f.getCanonicalPath(), "remove*" + f.getName());
        _ID = createID();
    }

    /** Setter method, changes the ID of the parent
     * commit for this commit.
     * @param id of parent commit*/
    public void setParent(String id) {
        _parent = id;
    }

    /** Setter method, changes the message of this commit.
     * @param message of the commit*/
    public void setMessage(String message) {
        _message = message;
    }

}

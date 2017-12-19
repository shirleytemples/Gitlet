package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

/** This effectively acts as my "Tree" class,
 * the Repo is the overseer of the entire .gitlet
 * repository.
 *
 * @author Max Miranda*/
public class Repo implements Serializable {

    /*********************** INIT ****************************/

    /** Creates a new Gitlet version-control system in the
     * current directory. This system will automatically start
     * with one commit: a commit that contains no files. It will have
     * a single branch: master, which initially points to this initial
     * commit, and master will be the current branch. */
    public Repo() {
        Commit initial = Commit.initialCommit();
        File gitlet = new File(".gitlet");
        gitlet.mkdir();
        File commits = new File(".gitlet/commits");
        commits.mkdir();
        File staging = new File(".gitlet/staging");
        staging.mkdir();

        String id = initial.getUniversalID();
        File initialFile = new File(".gitlet/commits/" + id);
        Utils.writeContents(initialFile, Utils.serialize(initial));
        _head = "master";
        _branches = new HashMap<String, String>();
        _branches.put("master", initial.getUniversalID());

        _stagingArea = new HashMap<String, String>();
        _untrackedFiles = new ArrayList<String>();
    }

    /*********************** LOG ****************************/

    /** Description: Starting at the current head commit,
     * display information about each commit backwards along the commit
     * tree until the initial commit, following the first parent commit
     * links, ignoring any second parents found in merge commits.
     * This set of commit nodes is called the commit's history. For
     * every node in this history, the information it should display
     * is the commit id, the time the commit was made, and the commit
     * message.
     */
    public void logCommits() {
        String head = getHead();
        while (head != null) {
            Commit first = uidToCommit(head);
            printACommit(head);
            head = first.getParentID();
        }
    }

    /** Takes in a UID for a commit, and prints out the commit,
     * what it prints out depends on whether it is a merge
     * commit or a regular commit. */
    public void printACommit(String uid) {
        Commit comm = uidToCommit(uid);
        if (comm.getParents() != null && comm.getParents().length > 1) {
            System.out.println("===");
            System.out.println("commit " + uid);
            String short1 = comm.getParents()[0].substring(0, 7);
            String short2 = comm.getParents()[1].substring(0, 7);
            System.out.println("Merge: " + short1 + " " + short2);
            System.out.println("Date: " + comm.getTimestamp());
            System.out.println(comm.getMessage());
            System.out.println();
        } else {
            System.out.println("===");
            System.out.println("commit " + uid);
            System.out.println("Date: " + comm.getTimestamp());
            System.out.println(comm.getMessage());
            System.out.println();
        }
    }

    /*********************** ADD ****************************/

    /** Takes in a String S.  */
    public void add(String s) {
        File f = new File(s);
        if (!f.exists()) {
            Utils.message("File does not exist.");
            throw new GitletException();
        }
        String fileHash = Utils.sha1(Utils.readContentsAsString(f));
        Commit mostRecent = uidToCommit(getHead());
        HashMap<String, String> files = mostRecent.getFiles();

        File stagingBlob = new File(".gitlet/staging/" + fileHash);
        boolean b = files == null;
        if (b || !files.containsKey(s) || !files.get(s).equals(fileHash)) {
            _stagingArea.put(s, fileHash);
            String contents = Utils.readContentsAsString(f);
            Utils.writeContents(stagingBlob, contents);
        } else {
            if (stagingBlob.exists()) {
                _stagingArea.remove(s);
            }
        }
        if (_untrackedFiles.contains(s)) {
            _untrackedFiles.remove(s);
        }
    }

    /*********************** COMMIT ****************************/

    /**
     *Takes in a String MSG.
     */
    public void commit(String msg) {
        if (msg.trim().equals("")) {
            Utils.message("Please enter a commit message.");
            throw new GitletException();
        }
        Commit mostRecent = uidToCommit(getHead());
        HashMap<String, String> trackedFiles = mostRecent.getFiles();

        if (trackedFiles == null) {
            trackedFiles = new HashMap<String, String>();
        }

        if (_stagingArea.size() != 0 || _untrackedFiles.size() != 0) {
            for (String fileName : _stagingArea.keySet()) {
                trackedFiles.put(fileName, _stagingArea.get(fileName));
            }
            for (String fileName : _untrackedFiles) {
                trackedFiles.remove(fileName);
            }
        } else {
            Utils.message("No changes added to the commit.");
            throw new GitletException();
        }
        String[] parent = new String[]{mostRecent.getUniversalID()};
        Commit newCommit = new Commit(msg, trackedFiles, parent, true);
        String s = newCommit.getUniversalID();
        File newCommFile = new File(".gitlet/commits/" + s);
        Utils.writeObject(newCommFile, newCommit);

        _stagingArea = new HashMap<String, String>();
        _untrackedFiles = new ArrayList<String>();
        _branches.put(_head, newCommit.getUniversalID());
    }

    /** Exactly like the regular commit function, but
     * used for merge commits, takes in a String MSG, and
     * a set of PARENTS. */
    public void commit(String msg, String[] parents) {
        if (msg.trim().equals("")) {
            Utils.message("Please enter a commit message.");
            throw new GitletException();
        }
        Commit mostRecent = uidToCommit(getHead());
        HashMap<String, String> trackedFiles = mostRecent.getFiles();

        if (trackedFiles == null) {
            trackedFiles = new HashMap<String, String>();
        }

        if (_stagingArea.size() != 0 || _untrackedFiles.size() != 0) {
            for (String fileName : _stagingArea.keySet()) {
                trackedFiles.put(fileName, _stagingArea.get(fileName));
            }
            for (String fileName : _untrackedFiles) {
                trackedFiles.remove(fileName);
            }
        } else {
            Utils.message("No changes added to the commit.");
            throw new GitletException();
        }
        Commit newCommit = new Commit(msg, trackedFiles, parents, true);
        String s = newCommit.getUniversalID();
        File newCommFile = new File(".gitlet/commits/" + s);
        Utils.writeObject(newCommFile, newCommit);

        _untrackedFiles = new ArrayList<String>();
        _stagingArea = new HashMap<String, String>();
        _branches.put(_head, newCommit.getUniversalID());
    }
    /*********************** REMOVE ****************************/

    /** Unstage the file if it is currently staged. If the file is
     * tracked in the current commit, mark it to indicate that it is
     * not to be included in the next commit (presumably you would store
     * this mark somewhere in the .gitlet directory), and remove the file
     * from the working directory if the user has not already done so
     * (do not renove it unless it is tracked in the current commit).
     ** Takes in a String ARG.
     * */
    public void rm(String arg) {
        File file = new File(arg);
        Commit mostRecent = uidToCommit(getHead());
        HashMap<String, String> trackedFiles = mostRecent.getFiles();
        if (!file.exists() && !trackedFiles.containsKey(arg)) {
            Utils.message("File does not exist.");
            throw new GitletException();
        }
        boolean changed = false;
        if (_stagingArea.containsKey(arg)) {
            _stagingArea.remove(arg);
            changed = true;
        }
        if (trackedFiles != null && trackedFiles.containsKey(arg)) {
            _untrackedFiles.add(arg);
            File toRemove = new File(arg);
            Utils.restrictedDelete(toRemove);
            changed = true;
        }
        if (!changed) {
            Utils.message("No reason to remove the file.");
            throw new GitletException();
        }
    }

    /*********************** GLOBAL LOG **************************/
    /** Takes no arguments, simply prints out all of the commits
     * that have ever occurred. */
    public void globalLog() {
        File commitFolder = new File(".gitlet/commits");
        File[] commits = commitFolder.listFiles();

        for (File file : commits) {
            printACommit(file.getName());
        }
    }

    /*********************** FIND ****************************/

    /** Takes in a MSG. */
    public void find(String msg) {
        File commitFolder = new File(".gitlet/commits");
        File[] commits = commitFolder.listFiles();
        boolean found = false;

        for (File file : commits) {
            Commit comm = uidToCommit(file.getName());
            if (comm.getMessage().equals(msg)) {
                System.out.println(file.getName());
                found = true;
            }
        }
        if (!found) {
            Utils.message("Found no commit with that message.");
            throw new GitletException();
        }
    }

    /*********************** STATUS ****************************/

    /** This will print out the status of a repository. */
    public void status() {
        System.out.println("=== Branches ===");
        Object[] keys = _branches.keySet().toArray();
        Arrays.sort(keys);
        for (Object branch : keys) {
            if (branch.equals(_head)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        Object[] stages = _stagingArea.keySet().toArray();
        Arrays.sort(stages);
        for (Object staged : stages) {
            System.out.println(staged);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        Object[] untracks = _untrackedFiles.toArray();
        Arrays.sort(untracks);
        for (Object removed : untracks) {
            System.out.println(removed);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    /*********************** CHECKOUT ****************************/

    /** Takes in a String[] ARGS.
     */
    public void checkout(String[] args) {
        String commID;
        String fileName;
        if (args.length == 2 && args[0].equals("--")) {
            fileName = args[1];
            commID = getHead();
        } else if (args.length == 3 && args[1].equals("--")) {
            commID = args[0];
            fileName = args[2];
        } else {
            Utils.message("Incorrect operands");
            throw new GitletException();
        }
        commID = convertShortenedID(commID);
        Commit comm = uidToCommit(commID);
        HashMap<String, String> trackedFiles = comm.getFiles();
        if (trackedFiles.containsKey(fileName)) {
            File f = new File(fileName);
            String p = ".gitlet/staging/";
            String blobFileName = p + trackedFiles.get(fileName);
            File g = new File(blobFileName);
            String contents = Utils.readContentsAsString(g);
            Utils.writeContents(f, contents);
        } else {
            Utils.message("File does not exist in that commit.");
            throw new GitletException();
        }
    }

    /** Takes in a shortened String ID and returns a String
     * of the full length ID. */
    private String convertShortenedID(String id) {
        if (id.length() == Utils.UID_LENGTH) {
            return id;
        }
        File commitFolder = new File(".gitlet/commits");
        File[] commits = commitFolder.listFiles();

        for (File file : commits) {
            if (file.getName().contains(id)) {
                return file.getName();
            }
        }
        Utils.message("No commit with that id exists.");
        throw new GitletException();
    }

    /** This is the third use case for checkout.
     * It takes in a BRANCHNAME. */
    public void checkout(String branchName) {
        if (!_branches.containsKey(branchName)) {
            Utils.message("No such branch exists.");
            throw new GitletException();
        }
        if (_head.equals(branchName)) {
            String s = "No need to checkout the current branch.";
            Utils.message(s);
            throw new GitletException();
        }
        String commID = _branches.get(branchName);
        Commit comm = uidToCommit(commID);
        HashMap<String, String> files = comm.getFiles();
        String pwdString = System.getProperty("user.dir");
        File pwd = new File(pwdString);
        checkForUntracked(pwd);
        for (File file : pwd.listFiles()) {
            if (files == null) {
                Utils.restrictedDelete(file);
            } else {
                boolean b = !files.containsKey(file.getName());
                if (b && !file.getName().equals(".gitlet")) {
                    Utils.restrictedDelete(file);
                }
            }
        }
        if (files != null) {
            for (String file : files.keySet()) {
                String g = ".gitlet/staging/" + files.get(file);
                File f = new File(g);
                String contents = Utils.readContentsAsString(f);
                Utils.writeContents(new File(file), contents);
            }
        }
        _stagingArea = new HashMap<String, String>();
        _untrackedFiles = new ArrayList<String>();
        _head = branchName;

    }

    /** This function takes in the present working directory
     * PWD and will determine if there are untracked files
     * that mean that this checkout or Merge operation can't
     * continue. */
    private void checkForUntracked(File pwd) {
        String s;
        s = "There is an untracked file in the way; ";
        s += "delete it or add it first.";
        Commit mostRecent = uidToCommit(getHead());
        HashMap<String, String> trackedFiles = mostRecent.getFiles();
        for (File file : pwd.listFiles()) {
            if (trackedFiles == null) {
                if (pwd.listFiles().length > 1) {
                    Utils.message(s);
                    throw new GitletException();
                }
            } else {
                boolean b = !trackedFiles.containsKey(file.getName());
                boolean c = !_stagingArea.containsKey(file.getName());
                if (b && !file.getName().equals(".gitlet") && c) {
                    Utils.message(s);
                    throw new GitletException();
                }
            }
        }
    }

    /*********************** BRANCH *****************/

    /**
     * Takes in a String ARG. And Creates a new branch
     * of that name.
     */
    public void branch(String arg) {
        if (!_branches.containsKey(arg)) {
            _branches.put(arg, getHead());
        } else {
            Utils.message("A branch with that name already exists.");
            throw new GitletException();
        }
    }

    /*********************** REMOVE BRANCH *********************/

    /** Takes in a string ARG, and removes that branch.
     *  */
    public void rmBranch(String arg) {
        if (_head.equals(arg)) {
            Utils.message("Cannot remove the current branch.");
            throw new GitletException();
        }
        if (_branches.containsKey(arg)) {
            _branches.remove(arg);
        } else {
            Utils.message("A branch with that name does not exist.");
            throw new GitletException();
        }
    }

    /*********************** RESET ****************************/

    /**
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     * See the intro for an example of what happens to the head pointer
     * after using reset. The [commit id] may be abbreviated as for checkout.
     * The staging area is cleared. The command is essentially checkout
     * of an arbitrary commit that also changes the current branch head.
     * Takes in a String COMMID.
     */
    public void reset(String commID) {
        commID = convertShortenedID(commID);
        Commit comm = uidToCommit(commID);
        HashMap<String, String> files = comm.getFiles();

        String pwdString = System.getProperty("user.dir");
        File pwd = new File(pwdString);
        checkForUntracked(pwd);

        for (File file : pwd.listFiles()) {
            if (!files.containsKey(file.getName())) {
                Utils.restrictedDelete(file);
            }
        }
        for (String file : files.keySet()) {
            File f = new File(".gitlet/staging/" + files.get(file));
            String contents = Utils.readContentsAsString(f);
            Utils.writeContents(new File(file), contents);
        }
        _stagingArea = new HashMap<String, String>();
        _branches.put(_head, commID);
    }

    /*********************** MERGE ****************************/

    /**
     * Failure case not yet addressed: If an untracked file in the
     * current commit would be overwritten or delted by the merge,
     * print There is an untracked file in the way; delete it or add it
     * first. and exit; perform this check before doing anything else.
     * Takes in a string BRANCHNAME. */
    public void merge(String branchName) {
        if (_stagingArea.size() != 0 || _untrackedFiles.size() != 0) {
            Utils.message("You have uncommitted changes.");
            throw new GitletException();
        }
        if (!_branches.containsKey(branchName)) {
            Utils.message("A branch with that name does not exist.");
            throw new GitletException();
        }
        if (branchName.equals(_head)) {
            Utils.message("Cannot merge a branch with itself.");
            throw new GitletException();
        }
        String split = splitPoint(branchName, _head);
        if (split.equals(_branches.get(branchName))) {
            Utils.message("Given branch is an ancestor of the current branch.");
            return;
        }
        if (split.equals(_branches.get(_head))) {
            _branches.put(_head, _branches.get(branchName));
            Utils.message("Current branch fast-forwarded.");
            return;
        }

        Commit splitCommit = uidToCommit(split);
        HashMap<String, String> splitFiles = splitCommit.getFiles();

        middleMerge(branchName);

        Commit currComm = uidToCommit(getHead());
        HashMap<String, String> current = currComm.getFiles();
        Commit givenComm = uidToCommit(_branches.get(branchName));
        HashMap<String, String> given = givenComm.getFiles();


        for (String fileName : given.keySet()) {
            if (!splitFiles.containsKey(fileName)) {
                if (!current.containsKey(fileName)) {
                    String b = _branches.get(branchName);
                    checkout(new String[] {b, "--", fileName});
                    _stagingArea.put(fileName, given.get(fileName));
                } else if (!given.containsKey(fileName)) {
                    continue;
                } else if (mo(fileName, given, current)) {
                    String p = ".gitlet/staging/";
                    File c = new File(p + current.get(fileName));
                    File g = new File(p + given.get(fileName));
                    String contents = "<<<<<<< HEAD\n";
                    contents += Utils.readContentsAsString(c);
                    contents += "=======\n";
                    contents += Utils.readContentsAsString(g) + ">>>>>>>";
                    Utils.writeContents(new File(fileName), contents);
                    add(fileName);
                    Utils.message("Encountered a merge conflict.");
                }
            }
        }
        String[] parents = new String[]{getHead(), _branches.get(branchName)};
        commit("Merged " + branchName + " into " + _head + ".", parents);
    }

    /** Splitting up the merge. Need a BRANCHNAME. */
    private void middleMerge(String branchName) {
        String split = splitPoint(branchName, _head);
        Commit splitCommit = uidToCommit(split);
        HashMap<String, String> splitFiles = splitCommit.getFiles();
        Commit currComm = uidToCommit(getHead());
        HashMap<String, String> current = currComm.getFiles();
        Commit givenComm = uidToCommit(_branches.get(branchName));
        HashMap<String, String> given = givenComm.getFiles();

        String pwdString = System.getProperty("user.dir");
        File pwd = new File(pwdString);
        checkForUntracked(pwd);

        for (String fileName : splitFiles.keySet()) {
            boolean presentInGiven = given.containsKey(fileName);
            boolean modifiedInCurrent = mo(fileName, splitFiles, current);
            boolean modifiedInGiven = mo(fileName, splitFiles, given);
            if (!modifiedInCurrent) {
                if (!presentInGiven) {
                    Utils.restrictedDelete(new File(fileName));
                    rm(fileName);
                    continue;
                }
                if (modifiedInGiven) {
                    String b = _branches.get(branchName);
                    checkout(new String[]{b, "--", fileName});
                    add(fileName);
                }
            }
            if (modifiedInCurrent && modifiedInGiven) {
                if (mo(fileName, given, current)) {
                    mergeConflict(branchName, fileName);
                }
            }
        }
    }

    /** This has a BRANCHNAME and a FILENAME. */
    private void mergeConflict(String branchName, String fileName) {
        String split = splitPoint(branchName, _head);
        Commit splitCommit = uidToCommit(split);
        HashMap<String, String> splitFiles = splitCommit.getFiles();
        Commit currComm = uidToCommit(getHead());
        HashMap<String, String> current = currComm.getFiles();
        Commit givenComm = uidToCommit(_branches.get(branchName));
        HashMap<String, String> given = givenComm.getFiles();
        String p = ".gitlet/staging/";
        File c;
        String cContents;
        if (current.containsKey(fileName)) {
            c = new File(p + current.get(fileName));
            cContents = Utils.readContentsAsString(c);
        } else {
            c = null;
            cContents = "";
        }
        File g;
        String gContents;
        if (given.containsKey(fileName)) {
            g = new File(p + given.get(fileName));
            gContents = Utils.readContentsAsString(g);
        } else {
            g = null;
            gContents = "";
        }
        String contents = "<<<<<<< HEAD\n";
        contents += cContents;
        contents += "=======\n" + gContents;
        contents += ">>>>>>>\n";
        Utils.writeContents(new File(fileName), contents);
        add(fileName);
        Utils.message("Encountered a merge conflict.");
    }
    /** Takes in two branch names, BRANCH1 and BRANCH2. Returns the
     * SHA ID of the common ancestor commit. */
    private String splitPoint(String branch1, String branch2) {
        ArrayList<String> branch1Commits = new ArrayList<String>();
        ArrayList<String> branch2Commits = new ArrayList<String>();

        String parent1 = _branches.get(branch1);
        String parent2 = _branches.get(branch2);

        while (parent1 != null) {
            branch1Commits.add(parent1);
            Commit comm1 = uidToCommit(parent1);
            parent1 = comm1.getParentID();
        }
        while (parent2 != null) {
            branch2Commits.add(parent2);
            Commit comm2 = uidToCommit(parent2);
            parent2 = comm2.getParentID();
        }
        for (String commit : branch1Commits) {
            if (branch2Commits.contains(commit)) {
                return commit;
            }
        }
        return "";
    }

    /** Returns a boolean if the file with name F has been modified from
     * branch H to branch I. */
    boolean mo(String f, HashMap<String, String> h, HashMap<String, String> i) {
        if (h.containsKey(f) && i.containsKey(f)) {
            String hashF1 = h.get(f);
            String hashF2 = i.get(f);
            if (!hashF1.equals(hashF2)) {
                return true;
            }
        } else if (h.containsKey(f) || i.containsKey(f)) {
            return true;
        }
        return false;
    }

    /*********************** HELPERS ****************************/

    /** This is how we are going to be capable of returns back and forth
     * in between each hash and the corresponding commit. Takes in a
     * String UID, and returns the commit object that corresponds
     * to that UID. */
    public Commit uidToCommit(String uid) {
        File f = new File(".gitlet/commits/" + uid);
        if (f.exists()) {
            return Utils.readObject(f, Commit.class);
        } else {
            Utils.message("No commit with that id exists.");
            throw new GitletException();
        }
    }

    /** Returns the uid of the current head which
     * corresponds to the head branch. */
    public String getHead() {
        return _branches.get(_head);
    }

    /** Returns branches. */
    public HashMap<String, String> getBranches() {
        return _branches;
    }

    /** Returns _stagingArea. */
    public HashMap<String, String> getStagingArea() {
        return _stagingArea;
    }

    /** Returns untrackedFiles. */
    public ArrayList<String> getUntrackedFiles() {
        return _untrackedFiles;
    }
    /** Overseer of entire tree structure, each branch has a name (String)
     * and a hash ID of its current position so that we can find the commit
     * that it's pointing to.*/
    private HashMap<String, String> _branches;

    /** The head pointer that corresponds to the branch that actually will be
     * pointing at the commit that we want . */
    private String _head;

    /** Staging Area, maps the name of the file, useful for figuring out
     * whether we need to swap it out for existing file in commit, or add
     * it entirely new. */
    private HashMap<String, String> _stagingArea;

    /** Untracked files are like the opposite of the Staging Area,
     * these are files that WERE tracked before, and now, for the
     * next commit, they're not going to be added. */
    private ArrayList<String> _untrackedFiles;
}

package gitlet;
import java.io.File;
import java.util.Arrays;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Max Miranda
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                Utils.message("Please enter a command.");
                throw new GitletException();
            }
            if (validCommand(args[0])) {
                String[] operands = Arrays.copyOfRange(args, 1, args.length);
                if (repoInitialized()) {
                    myRepo = recoverMyRepo();
                    hi(args, operands);
                    File mr = new File(MRPATH);
                    Utils.writeObject(mr, myRepo);
                } else {
                    if (args[0].equals("init")) {
                        myRepo = new Repo();
                        File mr = new File(MRPATH);
                        Utils.writeObject(mr, myRepo);
                    } else {
                        String s;
                        s = "Not in an initialized Gitlet directory.";
                        Utils.message(s);
                        throw new GitletException();
                    }
                }
            } else {
                Utils.message("No command with that name exists.");
                throw new GitletException();
            }
        } catch (GitletException e) {
            System.exit(0);
        }

    }

    /** Checks that there is not already a .gitlet directory
     * within this directory. Returns boolean true if .gitlet is
     * already in this directory. */
    public static boolean repoInitialized() {
        String f = System.getProperty("user.dir");
        File tmpDir = new File(f + "/.gitlet");
        if (tmpDir.exists()) {
            return true;
        }
        return false;
    }

    /** Takes in a string ARG word, will return whether or not
     * it is a valid command. */
    private static boolean validCommand(String arg) {
        for (String command: commands) {
            if (arg.equals(command)) {
                return true;
            }
        }
        return false;
    }

    /** Takes in String[] ARGS and String OPERANDS. */
    private static void hi(String[] args, String[] operands) {
        String already = "A Gitlet version-control system "
                + "already exists in the current directory.";
        switch (args[0]) {
        case "init":
            Utils.message(already);
            throw new GitletException();
        case "add":
            myRepo.add(operands[0]);
            break;
        case "commit":
            myRepo.commit(operands[0]);
            break;
        case "rm":
            myRepo.rm(operands[0]);
            break;
        case "log":
            myRepo.logCommits();
            break;
        case "global-log":
            myRepo.globalLog();
            break;
        case "find":
            myRepo.find(operands[0]);
            break;
        case "status":
            myRepo.status();
            break;
        case "checkout":
            if (operands.length == 1) {
                myRepo.checkout(operands[0]);
            } else {
                myRepo.checkout(operands);
            }
            break;
        case "branch":
            myRepo.branch(operands[0]);
            break;
        case "rm-branch":
            myRepo.rmBranch(operands[0]);
            break;
        case "reset":
            myRepo.reset(operands[0]);
            break;
        case "merge":
            myRepo.merge(operands[0]);
            break;
        default:
            Utils.message("something wrong");
        }
    }
    /** Will do the work of actually saving our information
     * from the repo. It returns the existing repo, assuming
     * that there is one. */
    public static Repo recoverMyRepo() {
        File mr =  new File(MRPATH);
        return Utils.readObject(mr, Repo.class);
    }

    /********************** VARIABLES ************************/

    /** Returns myRepo.*/
    public Repo getMyRepo() {
        return myRepo;
    }

    /** Returns commands.*/
    public String[] getCommands() {
        return commands;
    }

    /** Returns repo's path. */
    public String getMrpath() {
        return MRPATH;
    }
    /** Array of possible valid commands. */
    private static String[] commands = new String[] {"init", "add",
        "commit", "rm", "log", "global-log",
        "find", "status", "checkout",
        "branch", "rm-branch", "reset", "merge"};

    /** The thing that controls everything. */
    private static Repo myRepo;

    /** Path to that Repo's file. */
    private static final String MRPATH = ".gitlet/myrepo";


}

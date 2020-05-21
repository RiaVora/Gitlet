package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Ria Vora
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            exitWithError("Please enter a command.", 0);
        }
        switch (args[0]) {
        case "init":
            init(args);
            break;
        case "add":
            add(args);
            break;
        case "commit":
            commit(args);
            break;
        case "checkout":
            checkout(args);
            break;
        case "branch":
            branch(args);
            break;
        case "log":
            log(args);
            break;
        case "global-log":
            globalLog(args);
            break;
        case "find":
            find(args);
            break;
        case "rm-branch":
            removeBranch(args);
            break;
        case "status":
            status(args);
            break;
        case "rm":
            remove(args);
            break;
        case "reset":
            reset(args);
            break;
        case "merge":
            merge(args); break;
        case "print":
            if (new File(".", args[1]).exists()) {
                pln(Utils.readContentsAsString(new File(".", args[1])));
            } else {
                pln("File does not exist in working directory");
            }
            break;
        default:
            exitWithError("No command with that name exists.", 0);
        }
    }


    /** Checks that the necessary folders exist,
     * and if they don't, error saying that the gitlet.
     * directory has not been initialized*/
    public static void checkInit() {
        if (!checkFolders()) {
            exitWithError("Not in an initialized Gitlet directory.", 0);
        }

    }

    /** Checks that all of the necessary folders exist
     * in the given directory.
     * @return boolean is whether they exist*/
    public static boolean checkFolders() {
        return GITLET_FOLDER.exists() && COMMITS_FOLDER.exists()
                && HEAD_FILE.exists() && STAGE_FOLDER.exists()
                && OBJECTS_FOLDER.exists() && CURRENT_COMMIT_FILE.exists()
                && BRANCHES_FOLDER.exists();
    }



    /* INIT COMMAND */



    /** Initializes the .gitlet folder and all of the
     * following necessary folders and files.
     * @param args is user input of {'init'}*/
    public static void init(String[] args) {

        validateNumArgs("init", args, 1);

        if (!GITLET_FOLDER.exists()) {
            GITLET_FOLDER.mkdir();
        } else {
            exitWithError("A Gitlet version-control system"
                    + "already exists in the current directory.", 0);
        }

        BRANCHES_FOLDER.mkdir();
        COMMITS_FOLDER.mkdir();
        STAGE_FOLDER.mkdir();
        OBJECTS_FOLDER.mkdir();

        Commit firstCommit = new Commit(true);
        File commitFile = Utils.join(COMMITS_FOLDER,
                firstCommit.getID() + ".txt");
        Utils.writeObject(commitFile, firstCommit);

        File master = Utils.join(BRANCHES_FOLDER, "master.txt");
        Utils.writeContents(master, firstCommit.getID());

        Utils.writeContents(HEAD_FILE, "branches/master.txt");

        Utils.writeObject(CURRENT_COMMIT_FILE, new Commit());
    }



    /* ADD COMMAND */



    /** The add method called when the user adds a file
     * to be committed, which updates the .gitlet folder
     * of stage and adds the file pathway to the current commit.
     * @param args is user input of {'add', file}*/
    public static void add(String[] args) throws IOException {

        checkInit();

        validateNumArgs("add", args, 2);

        File add = Utils.join(".", args[1]);
        if (!add.exists()) {
            exitWithError("File does not exist.", 0);
        }

        Commit currentCommit = Utils.readObject(CURRENT_COMMIT_FILE,
                Commit.class);
        String contents = Utils.readContentsAsString(add);
        File stage = Utils.join(STAGE_FOLDER, Utils.sha1(contents));
        Commit headCommit = getHeadCommit();
        if (currentCommit.getFileToID().containsKey(
                add.getCanonicalPath())) {
            File removedFile = getFile(currentCommit.getIDFromFile(
                    add.getCanonicalPath()), STAGE_FOLDER.listFiles());
            removedFile.delete();
            currentCommit.getFileToID().remove(add.getCanonicalPath());

        } else if (!headCommit.getIDFromFile(add.getCanonicalPath())
                .equals(Utils.sha1(contents))) {
            Utils.writeContents(stage, contents);
            currentCommit.addFile(add);
        }
        Utils.writeObject(CURRENT_COMMIT_FILE, currentCommit);
    }



    /* COMMIT COMMAND */



    /** The commit method called when the user commits,
     * which updates the .gitlet folders of objects, stage,
     * commits, branches, and the file commit.txt for the
     * new commit based off of the added files.
     * @param args is user input of {'commit'} or
     * {'commit', '-m', message}*/
    public static void commit(String[] args) {

        checkInit();

        Commit commit = Utils.readObject(CURRENT_COMMIT_FILE, Commit.class);

        String secondParent = "";
        if (args.length == 3 && args[0].equals("commit-merge")) {
            secondParent = args[2];
        } else if (args.length != 2 || args[1].isEmpty()) {
            exitWithError("Please enter a commit message.", 0);
        }
        commit.setMessage(args[1]);


        File[] files = STAGE_FOLDER.listFiles();

        if (files.length == 0) {
            exitWithError("No changes added to the commit.", 0);
        }

        for (File file: files) {
            addToObjects(file);
        }

        saveCommit(commit, secondParent);

        Utils.writeObject(CURRENT_COMMIT_FILE, new Commit());

    }

    /** A helper method to duplicate a file's contents
     * into the objects folder and delete its current
     * position.
     * @param file is the file in the stage folder*/
    public static void addToObjects(File file) {
        File object = Utils.join(OBJECTS_FOLDER, file.getName());
        Utils.writeContents(object, Utils.readContents(file));

        file.delete();
    }

    /** A helper method to update the parent of the current commit,
     * save the commit as the head commit, and add the commit
     * to the commits folder.
     * @param c is the current commit
     * @param secondParent is the secondParent of the commit*/
    public static void saveCommit(Commit c, String secondParent) {
        c.createTimestamp();

        Commit pastCommit = getHeadCommit();

        if (secondParent.isEmpty()) {
            c.setParent(pastCommit.getID());
        } else {
            c.setParent(pastCommit.getID() + " " + secondParent);
        }


        Utils.writeContents(getHeadBranch(), c.getID());

        File newCommit = Utils.join(COMMITS_FOLDER, c.getID() + ".txt");
        Utils.writeObject(newCommit, c);
    }

    /** Returns the file/pathway corresponding to the head branch from the
     * head.txt file.
     * @return File is the file/pathway*/
    public static File getHeadBranch() {
        String pathwayBranch = Utils.readContentsAsString(HEAD_FILE);
        return Utils.join(GITLET_FOLDER, pathwayBranch);
    }

    /** Returns the head commit based off of the branch pathway
     * and found from the commits in the commit folder.
     * @return Commit is the head commit*/
    public static Commit getHeadCommit() {
        File branchFile = getHeadBranch();
        String commitID = Utils.readContentsAsString(branchFile);
        File commitFile = Utils.join(COMMITS_FOLDER, commitID + ".txt");

        if (getFile(commitID + ".txt", COMMITS_FOLDER.listFiles()) == null) {
            exitWithError("the commit in ur branch "
                    + branchFile.getName() + " does not exist", -1);
        }

        return Utils.readObject(commitFile, Commit.class);
    }



    /* CHECKOUT COMMAND */



    /** The checkout method reverts a file back to the last commit/given commit,
     * or converts all of the files back to the version in given branch.
     * @param args is user input of {'checkout', '--', fileName} or
     * {'checkout', commitID, '--', fileName} or {'checkout', branchName}*/
    public static void checkout(String[] args) throws IOException {
        checkInit();
        if (args.length == 3 && args[1].equals("--")) {
            Commit headCommit = getHeadCommit();
            File file = Utils.join(new File("."), args[2]);
            revertFile(file, headCommit);
        } else if (args.length == 4 && args[2].equals("--")) {
            File commitFile;
            if (args[1].length() < Utils.UID_LENGTH) {
                commitFile = getFileShort(args[1],
                        COMMITS_FOLDER.listFiles());
            } else {
                commitFile = getFile(args[1] + ".txt",
                        COMMITS_FOLDER.listFiles());
            }
            if (commitFile == null) {
                exitWithError("No commit with that id exists.", 0);
            }
            Commit commit = Utils.readObject(commitFile, Commit.class);
            File file = Utils.join(new File("."), args[3]);
            revertFile(file, commit);
        } else if (args.length == 2) {
            File branchFile = getFile(args[1] + ".txt",
                    BRANCHES_FOLDER.listFiles());
            if (branchFile == null) {
                exitWithError("No such branch exists.", 0);
            } else if (getHeadBranch().getName().equals(branchFile.getName())) {
                exitWithError("No need to checkout the current branch.", 0);
            }
            String commitID = Utils.readContentsAsString(branchFile);
            File commitFile = Utils.join(COMMITS_FOLDER, commitID + ".txt");
            Commit commit = Utils.readObject(commitFile, Commit.class);
            Commit headCommit = getHeadCommit();
            untrackedFileError(commit, headCommit);
            for (String filePath: commit.getFileToID().keySet()) {
                revertFile(Utils.join(new File(filePath)), commit);
            }
            for (String filePath: headCommit.getFileToID().keySet()) {
                if (!commit.getFileToID().containsKey(filePath)) {
                    (new File(filePath)).delete();
                }
            } resetStagingArea();
            Utils.writeContents(HEAD_FILE,
                    "branches/" + args[1] + ".txt");
        } else {
            exitWithError("Incorrect operands.", 0);
        }
    }

    /** Reverts a file back to its version in the given commit.
     * @param f is the name of the file
     * @param c is the commit that holds the desired version of
     * the file.*/
    public static void revertFile(File f, Commit c) throws IOException {
        if (!c.getFileToID().containsKey(f.getCanonicalPath())) {
            exitWithError("File does not exist in that commit.", 0);
        }
        String contentsID = c.getIDFromFile(f.getCanonicalPath());
        if (contentsID.substring(0, 7).equals("remove*")) {
            f.delete();
        } else {
            File file = getFile(contentsID, OBJECTS_FOLDER.listFiles());
            if (file == null) {
                exitWithError("File does not exist in that commit.", 0);
            }
            String contents = Utils.readContentsAsString(file);
            Utils.writeContents(f, contents);
        }
    }

    /** Returns the file matching the given name from
     * a given list of files, used to search through a
     * directory for a desired file.
     * @param name is the name of the desired file
     * @param files is an array of all the files in
     *              that directory
     * @return File is the desired file*/
    public static File getFile(String name, File[] files) {
        if (name.isEmpty()) {
            return null;
        }
        for (File file: files) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        return null;
    }

    /** Returns the file that's first characters match the given name
     * from a given list of files, used to search through a
     * directory for a desired file.
     * @param name is the name of the desired file
     * @param files is an array of all the files in
     *              that directory
     * @return File is the desired file*/
    public static File getFileShort(String name, File[] files) {
        if (name.isEmpty()) {
            return null;
        }
        for (File file: files) {
            if (file.getName().substring(0, name.length()).equals(name)) {
                return file;
            }
        }
        return null;
    }
    /** Resets the staging area by clearing the stage
     * folder and setting a new commit.*/
    public static void resetStagingArea() {
        for (File file: STAGE_FOLDER.listFiles()) {
            file.delete();
        }
        Utils.writeObject(CURRENT_COMMIT_FILE, new Commit());
    }
    /** A helper method to check for an untracked file error.
     * @param commit is given commit
     * @param headCommit is the given head commit*/
    public static void untrackedFileError(Commit commit, Commit headCommit) {
        for (String filePath: commit.getFileToID().keySet()) {
            if (new File(filePath).exists()) {
                String currentContents = Utils.sha1(
                        Utils.readContentsAsString(new File(filePath)));
                if (!headCommit.getFileToID().containsKey(filePath)
                        && !currentContents.equals(commit.getIDFromFile(
                        filePath))
                        && !(commit.getIDFromFile(filePath)).substring(0, 7).
                        equals("remove*")) {
                    exitWithError("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.", 0);
                }
            }
        }
    }




    /* BRANCH COMMAND */



    /** The branch method is called when the user adds a new
     * branch, using the current head commit, but does not
     * switch that branch to the head branch.
     * @param args is user input of {'branch', branchName}*/
    public static void branch(String[] args) {

        checkInit();

        validateNumArgs("branch", args, 2);

        File newBranch = Utils.join(BRANCHES_FOLDER, args[1] + ".txt");
        if (newBranch.exists()) {
            exitWithError("A branch with that name already exists.", 0);
        }
        Utils.writeContents(newBranch, getHeadCommit().getID());
    }



    /* LOG COMMAND */



    /** The log method is called when the user wants
     * to see a full list of all commits made.
     * @param args is user input of {'log'}*/
    public static void log(String[] args) {
        checkInit();

        validateNumArgs("log", args, 1);

        Commit headCommit = getHeadCommit();

        iterateCommits(headCommit);

    }

    /** Prints each commit in proper format.
     * @param commit is the commit to be printed*/
    public static void printCommit(Commit commit) {
        pln("===");
        pln("commit " + commit.getID());
        pln("Date: " + commit.getTimestamp());
        pln(commit.getMessage() + "\n");
    }

    /** Iterates through the commit using its
     * parent.
     * @param commit is the commit*/
    public static void iterateCommits(Commit commit) {
        while (commit != null) {
            printCommit(commit);
            String parent = commit.getParent();
            if (parent.length() > Utils.UID_LENGTH) {
                parent = parent.substring(0, Utils.UID_LENGTH);
            }
            File parentFile = getFile(parent + ".txt",
                    COMMITS_FOLDER.listFiles());
            if (parentFile != null) {
                commit = Utils.readObject(parentFile, Commit.class);
            } else {
                commit = null;
            }
        }
    }



    /* GLOBAL-LOG COMMAND */



    /** The global log method is used to print out all
     * of the commits ever made in this repository.
     * @param args is user input of {'global-log'}*/
    public static void globalLog(String[] args) {

        checkInit();

        validateNumArgs("add", args, 1);

        for (File commitFile: COMMITS_FOLDER.listFiles()) {
            Commit commit = Utils.readObject(commitFile, Commit.class);
            printCommit(commit);
        }
    }



    /* FIND COMMAND */



    /** Find is used to return all commit IDS of commits matching
     * the given input message.
     * @param args is user input of {'find', commitMessage}*/
    public static void find(String[] args) {

        checkInit();

        validateNumArgs("add", args, 2);

        ArrayList<String> matchingCommits = new ArrayList<String>();
        for (File commitFile: COMMITS_FOLDER.listFiles()) {
            Commit commit = Utils.readObject(commitFile, Commit.class);
            if (commit.getMessage().equals(args[1])) {
                matchingCommits.add(commit.getID());
            }
        }

        if (matchingCommits.size() == 0) {
            exitWithError("Found no commit with that message", 0);
        } else {
            for (String commitID: matchingCommits) {
                System.out.println(commitID);
            }
        }
    }



    /* RM-BRANCH COMMAND */



    /** removeBranch is used for the rm-branch command to remove
     * the branch with the given name. It removes the branch as a
     * pointer, not all of the commits associated with that
     * branch.
     * @param args is user input of {'rm-branch', branchName}*/
    public static void removeBranch(String[] args) {

        checkInit();

        validateNumArgs("rm-branch", args, 2);

        File branchFile = getFile(args[1] + ".txt",
                BRANCHES_FOLDER.listFiles());
        if (branchFile == null) {
            exitWithError("A branch with that name does not exist.", 0);
        } else if (getHeadBranch().getName().equals(branchFile.getName())) {
            exitWithError("Cannot remove the current branch.", 0);
        } else {
            branchFile.delete();
        }
    }



    /* RM COMMAND */



    /** removeBranch is called when the user wants to delete
     * a file from the staging area and/or delete it from
     * the working directory.
     * @param args is user input of {'rm', fileName}*/
    public static void remove(String[] args) throws IOException {

        checkInit();

        validateNumArgs("add", args, 2);

        boolean notIn = true;
        Commit currentCommit = Utils.readObject(CURRENT_COMMIT_FILE,
                Commit.class);
        File removeFile = Utils.join(".", args[1]);
        String removeFilePathway = removeFile.getCanonicalPath();

        if (currentCommit.getFileToID().containsKey(removeFilePathway)) {
            Utils.join(STAGE_FOLDER, currentCommit.getIDFromFile(
                    removeFilePathway)).delete();
            currentCommit.getFileToID().remove(removeFilePathway);
            notIn = false;
        }

        if (getHeadCommit().getFileToID().containsKey(removeFilePathway)) {
            currentCommit.addRemoveFile(removeFile);
            File stageFile = Utils.join(STAGE_FOLDER,
                    currentCommit.getIDFromFile(removeFilePathway));
            if (removeFile.exists()) {
                Utils.writeContents(stageFile,
                        Utils.readContentsAsString(removeFile));
                removeFile.delete();
            } else {
                Utils.writeContents(stageFile, "");
            }
            notIn = false;
        }

        Utils.writeObject(CURRENT_COMMIT_FILE, currentCommit);

        if (notIn) {
            exitWithError("No reason to remove the file.", 0);
        }

    }



    /* STATUS COMMAND */



    /** status is used to view the status of all
     * added and removed files and branches.
     * @param args is user input of {'status'}*/
    public static void status(String[] args) throws IOException {

        checkInit();

        validateNumArgs("add", args, 1);

        printTitle("Branches");
        String headBranchName = getHeadBranch().getName();
        pln("*" + removeTXT(headBranchName));
        File[] files = BRANCHES_FOLDER.listFiles();
        Arrays.sort(files);
        for (File file: files) {
            if (!file.getName().equals(headBranchName)) {
                pln(removeTXT(file.getName()));
            }
        }
        pln("");

        printTitle("Staged Files");
        files = STAGE_FOLDER.listFiles();
        ArrayList<File> removedFiles = new ArrayList<File>();
        Arrays.sort(files);
        for (File file: files) {
            if (!file.getName().substring(0, 7).equals("remove*")) {
                printFile(file);
            } else {
                removedFiles.add(file);
            }
        }
        pln("");

        printTitle("Removed Files");
        for (File file: removedFiles) {
            printFile(file);
        }
        pln("");


        printTitle("Modifications Not Staged For Commit");
        HashMap<File, String> modifiedNotStaged = modifiedNotStaged();
        ArrayList<File> sortedFiles = new
                ArrayList<File>(modifiedNotStaged.keySet());
        Collections.sort(sortedFiles);
        for (File file: sortedFiles) {
            pln(file.getName() + " ("
                    + modifiedNotStaged.get(file) + ")");
        }
        pln("");

        printTitle("Untracked Files");
        ArrayList<File> untracked = untracked();
        Collections.sort(untracked);
        for (File file: untracked) {
            pln(file.getName());
        }
    }

    /** Prints the title with the proper notation.
     * @param title is given title*/
    public static void printTitle(String title) {
        pln("=== " + title + " ===");
    }

    /** Removes TXT from the end of a file name.
     * @param fileName is the name of the file
     * @return is the file name without the .txt*/
    public static String removeTXT(String fileName) {
        return fileName.substring(0, fileName.length() - 4);
    }

    /** Prints the file name from the given file ID using
     * the current commit.
     * @param file is the array of files*/
    public static void printFile(File file) {
        Commit commit = Utils.readObject(CURRENT_COMMIT_FILE, Commit.class);
        File foundFile = new File(commit.getFileFromID(file.getName()));
        pln(foundFile.getName());
    }

    /** Stores the files that fit the description of
     * modified and not staged in an Arraylist.
     * @return the ArrayList of modified and not
     * staged files*/
    public static HashMap<File, String> modifiedNotStaged() {
        HashMap<File, String> result = new HashMap<File, String>();
        Commit currentCommit = Utils.readObject(CURRENT_COMMIT_FILE,
                Commit.class);
        Commit headCommit = getHeadCommit();
        for (String filePath: headCommit.getFileToID().keySet()) {
            File f = new File(filePath);
            if (f.exists()) {
                if (!headCommit.getIDFromFile(filePath).equals(
                        Utils.sha1(Utils.readContentsAsString(f)))
                    && !currentCommit.getFileToID().containsKey(filePath)) {
                    result.put(f, "modified");
                }
            } else if (currentCommit.getFileToID().containsKey(filePath)) {
                if (!currentCommit.getIDFromFile(filePath).
                        substring(0, 7).equals("remove*")) {
                    result.put(f, "deleted");
                }
            } else if (!headCommit.getIDFromFile(filePath).
                    substring(0, 7).equals("remove*")) {
                result.put(f, "deleted");
            }
        }

        for (String filePath: currentCommit.getFileToID().keySet()) {
            File f = new File(filePath);
            if (!currentCommit.getIDFromFile(filePath).
                    substring(0, 7).equals("remove*") && f.exists()) {
                if (!currentCommit.getIDFromFile(filePath).
                        equals(Utils.sha1(Utils.readContentsAsString(f)))) {
                    result.put(f, "modified");
                }
            } else if (!currentCommit.getIDFromFile(filePath).
                    substring(0, 7).equals("remove*") && !f.exists()) {
                result.put(f, "deleted");
            }
        }

        return result;
    }

    /** Stores the files that fit the description of
     * untracked in an Arraylist.
     * @return the ArrayList of untracked files*/
    public static ArrayList<File> untracked() throws IOException {
        ArrayList<File> result = new ArrayList<File>();
        File currentDirectory = new File(".");
        Commit currentCommit = Utils.readObject(CURRENT_COMMIT_FILE,
                Commit.class);
        Commit headCommit = getHeadCommit();
        for (File f: currentDirectory.listFiles()) {
            if (!headCommit.getFileToID().containsKey(
                    f.getCanonicalPath())
                    && !f.getName().equals(".gitlet")) {
                if (!currentCommit.getFileToID().containsKey(
                        f.getCanonicalPath())) {

                    result.add(f);
                } else if (currentCommit.getIDFromFile(
                        f.getCanonicalPath()).substring(0, 7).
                        equals("remove*")
                    && f.exists()) {
                    result.add(f);
                }
            }
        }
        result.remove(GITLET_FOLDER);
        return result;
    }

    /* RESET COMMAND */



    /** reset is used to revert the git directory back to the
     * version of the given commit.
     * @param args is user input of {'reset', commitID}*/
    public static void reset(String[] args) throws IOException {

        checkInit();

        if (getFile(args[1]  + ".txt",
                COMMITS_FOLDER.listFiles()) == null) {
            exitWithError("No commit with that id exists.", 0);
        } else if (getHeadCommit().getID().equals(args[1])) {
            exitWithError("No need to reset to the current commit.", 0);
        }


        validateNumArgs("add", args, 2);

        String pastPathway = Utils.readContentsAsString(HEAD_FILE);

        String branchName = Utils.sha1("branchToBeUsedForCheckout");

        String[] input = {"branch", branchName};

        branch(input);

        Utils.writeContents(Utils.join(BRANCHES_FOLDER,
                branchName + ".txt"), args[1]);

        input[0] = "checkout";

        checkout(input);

        Utils.writeContents(HEAD_FILE, pastPathway);

        Utils.writeContents(getHeadBranch(), args[1]);


        input[0] = "rm-branch";
        removeBranch(input);
    }




    /* MERGE COMMAND */



    /** The merge method merges the files of both the
     * current branch and the given branch.
     * @param args is user input of {'merge', branchName}*/
    public static void merge(String[] args) throws IOException {
        checkInit();
        File branchFile = getFile(args[1] + ".txt",
                BRANCHES_FOLDER.listFiles());
        if (branchFile == null) {
            exitWithError("A branch with that name "
                    + "does not exist.", 0);
        } else if (getHeadBranch().getName().equals(branchFile.getName())) {
            exitWithError("Cannot merge a branch with"
                    + " itself.", 0);
        }
        if (STAGE_FOLDER.listFiles().length != 0) {
            exitWithError("You have uncommitted changes.",
                    0);
        }
        String commitID = Utils.readContentsAsString(branchFile);
        File commitFile = getFile(commitID + ".txt",
                COMMITS_FOLDER.listFiles());
        Commit commit = Utils.readObject(commitFile,
                Commit.class);
        Commit headCommit = getHeadCommit();
        untrackedFileError(commit, headCommit);
        Commit splitCommit = findSplitPoint(commit, headCommit);
        if (splitCommit == null) {
            exitWithError("Unknown error, split commit not found",
                    0);
        } else if (splitCommit.getID().equals(commitID)) {
            exitWithError("Given branch is an ancestor of the "
                   + "current branch.", 0);
        } else if (splitCommit.getID().equals(headCommit.getID())) {
            String[] input = {"checkout", args[1]};
            checkout(input);
            pln("Current branch fast-forwarded.");
        }
        checkCommit(commit, headCommit, splitCommit);
        for (String filePath: headCommit.getFileToID().keySet()) {
            if (!commit.getFileToID().containsKey(filePath)
                    && !splitCommit.getFileToID().containsKey(filePath)) {
                stageFile(new File(filePath), Utils.readContentsAsString
                        (new File(filePath)), false);
            }
        }
        checkSplitCommit(commit, headCommit, splitCommit);
        boolean conflict = checkMergeConflict(commit, headCommit);
        String[] input = {"commit-merge", "Merged " + args[1]
                + " into "
                + removeTXT(getHeadBranch().getName()) + ".", commitID};
        commit(input);
        if (conflict) {
            pln("Encountered a merge conflict.");
        }
    }

    /** The merge method merges the files of both the
     * current branch and the given branch.
     * @param f is given file
     * @param contents is the String contents to be put
     * in the staged file
     * @param remove is a boolean telling whether the file
     * should be staged for removal or addition*/
    public static void stageFile(File f, String contents,
                                 boolean remove) throws IOException {
        File stage = Utils.join(STAGE_FOLDER, Utils.sha1(contents));
        if (remove) {
            stage = Utils.join(STAGE_FOLDER,
                    "remove*" + f.getName());
        }
        Utils.writeContents(stage, contents);
        Commit currentCommit = Utils.readObject(CURRENT_COMMIT_FILE,
                Commit.class);
        if (remove) {
            currentCommit.addRemoveFile(f);
        } else {
            currentCommit.addFile(f);
        }
        Utils.writeObject(CURRENT_COMMIT_FILE, currentCommit);
    }

    /** A helper method to find the split point between the
     * two branches by iterating through the parents of the
     * commits.
     * @param branch1Commit is head commit of the given branch
     * @param branch2Commit is the head commit of the current branch
     * @return is the split commit*/
    public static Commit findSplitPoint(Commit branch1Commit,
                                        Commit branch2Commit) {
        Commit commit1 = branch1Commit;
        Commit commit2 = branch2Commit;
        while (commit1 != null && commit2 != null) {
            if (commit1.getID().equals(commit2.getID())
                    || commit2.getParent().equals(commit1.getID())) {
                return commit1;
            } else if (commit1.getParent().equals(commit2.getID())) {
                return commit2;
            }

            if (commit1.getParent().isEmpty()) {
                commit1 = null;
            } else {
                commit1 = Utils.readObject(getFile(
                        commit1.getParent() + ".txt",
                        COMMITS_FOLDER.listFiles()), Commit.class);
            }

            if (commit2.getParent().isEmpty()) {
                commit2 = null;
            } else {
                commit2 = Utils.readObject(getFile(
                        commit2.getParent() + ".txt",
                        COMMITS_FOLDER.listFiles()), Commit.class);
            }
        }

        return null;

    }

    /** A helper method to check whether a file has been modified
     * between two commit.
     * @param filePath is the pathway of the file
     * @param currentContentsID is contents of the file in
     * the start commit
     * @param start is the starting head commit
     * @param end is the ending commit, usually the split poijt
     * @return is whether the file has been modifiedt*/
    public static boolean checkFileModified(String filePath,
                                            String currentContentsID,
                                            Commit start, Commit end) {
        Commit current = start;
        while (!current.getID().equals(end.getID())) {
            if (current.getFileToID().containsKey(filePath)
                && !current.getIDFromFile(filePath).equals(currentContentsID)) {
                return true;
            }
            current = Utils.readObject(getFile(current.getParent()
                    + ".txt", COMMITS_FOLDER.listFiles()), Commit.class);
        }
        return false;
    }

    /** A helper method to check on the files
     * in the split commit.
     * @param commit is the given commit.
     * @param headCommit is given head commit
     * @param splitCommit is the given split commit*/
    public static void checkSplitCommit(Commit commit,
                                        Commit headCommit,
                                        Commit splitCommit) throws IOException {
        for (String filePath : splitCommit.getFileToID().keySet()) {
            File f = new File(filePath);
            if (!commit.getFileToID().containsKey(filePath)
                    && !checkFileModified(filePath, splitCommit.getIDFromFile(
                    filePath), headCommit, splitCommit)) {
                f.delete();
                Commit curr = Utils.readObject(CURRENT_COMMIT_FILE,
                        Commit.class);
                if (curr.getFileToID().containsKey(filePath)) {
                    Utils.join(STAGE_FOLDER, curr.getIDFromFile(
                            filePath)).delete();
                    curr.getFileToID().remove(filePath);
                }
            } else if (!headCommit.getFileToID().containsKey(filePath)
                    && checkFileModified(filePath, splitCommit.getIDFromFile(
                    filePath), commit, splitCommit)) {
                stageFile(f, "", true);
                f.delete();
            }
        }
    }

    /** A helper method to check for a merge conflict
     * and updated files accordingly.
     * @param commit is the given commit.
     * @param headCommit is given head commit
     * @return is a boolean showing whether there was a
     * merge conflict*/
    public static boolean checkMergeConflict(
            Commit commit,
            Commit headCommit) throws IOException {
        boolean conflict = false;
        for (String filePath: commit.getFileToID().keySet()) {
            if (headCommit.getFileToID().containsKey(filePath)
                    && !commit.getIDFromFile(filePath).equals(
                    headCommit.getIDFromFile(filePath))) {
                File headFileContents = getFile(headCommit.
                        getIDFromFile(filePath), OBJECTS_FOLDER.listFiles());
                File fileContents = getFile(commit.getIDFromFile(filePath),
                        OBJECTS_FOLDER.listFiles());
                String headContents = Utils.readContentsAsString(
                        headFileContents);
                String contents = Utils.readContentsAsString(fileContents);
                if (headFileContents.getName().substring(0, 7).
                        equals("remove*")) {
                    headContents = "";
                } else if (fileContents.getName().substring(0, 7).
                        equals("remove*")) {
                    contents = "";
                }
                String combined = "<<<<<<< HEAD\n" + headContents
                        + "=======\n" + contents + ">>>>>>>\n";
                Utils.writeContents(new File(filePath), combined);
                stageFile(new File(filePath), combined, false);
                conflict = true;
            }
        }
        return conflict;
    }


    /** A helper method that iterates through the commit
     * and checks if files need to be changed or updated.
     * @param commit is the given commit.
     * @param headCommit is given head commit
     * @param splitCommit is the given split commit*/
    public static void checkCommit(Commit commit, Commit
            headCommit, Commit splitCommit) throws IOException {
        for (String filePath: commit.getFileToID().keySet()) {
            File f = new File(filePath);
            boolean cond1 = splitCommit.getFileToID().containsKey(filePath)
                    && !commit.getIDFromFile(filePath).
                    equals(splitCommit.getIDFromFile(filePath))
                    && !checkFileModified(filePath,
                    splitCommit.getIDFromFile(filePath),
                    headCommit, splitCommit);
            boolean cond2 = !splitCommit.getFileToID().containsKey(filePath)
                    && !headCommit.getFileToID().containsKey(filePath)
                    && !f.exists();
            if (cond1 || cond2) {
                revertFile(f, commit);
                if (commit.getIDFromFile(filePath).substring(0, 7).
                        equals("remove*")) {
                    stageFile(f, "", true);
                } else {
                    stageFile(f, Utils.readContentsAsString(f), false);
                }
            }
        }
    }

    /** A helper method to print an error statement and exit
     * from the program.
     * @param args is the error statement
     * @param exitCode is the exit Code*/
    public static void exitWithError(String args, int exitCode) {
        pln(args);
        System.exit(exitCode);
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Invalid number of arguments for: %s.", cmd));
        }
    }

    /** Helper method for the printing method.
     * @param toPrint is string to be printed*/
    public static void pln(String toPrint) {
        System.out.println(toPrint);
    }


    /** The .gitlet folder storing all of the data. */
    static final File GITLET_FOLDER = new File(".gitlet");

    /** The head file pointing to the head branch. */
    static final File HEAD_FILE = new File(".gitlet/head.txt");

    /** The branches folder containing each of the branches. */
    static final File BRANCHES_FOLDER = new File(".gitlet/branches");

    /** The commits folder containing all of the commits. */
    static final File COMMITS_FOLDER = new File(".gitlet/commits");

    /** The stage folder containing all added files to be committed. */
    static final File STAGE_FOLDER = new File(".gitlet/stage");

    /** The objects folder containing all committed blobs (files). */
    static final File OBJECTS_FOLDER = new File(".gitlet/objects");

    /** The current commit file containing the serialized current commit. */
    static final File CURRENT_COMMIT_FILE = new File(".gitlet/commit.txt");



}


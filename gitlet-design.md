# Gitlet Design Document

**Name**: Ria Vora

## Classes and Data Structures

###Directory
This class stores a directory holding other directories and files

####Fields
1. String _pathway: A string representing the path of the file to the home directory
2. ArrayList _files: An ArrayList representing a linked list of the files and directories in this directory
3. HashMap _changed: A HashMap holding all of the changed files that need to be committed
4. ArrayList _commits: An ArrayList of all commits that have been done
5. String _timestamp: A string representing the timestamp of the latest commit
(could consider making a Timestamp object)
6. String name: The string name of the directory
    
###Commit
This class stores all of the information surrounded a commit
    
####Fields
1. ArrayList _files: An ArrayList representing all of the files involved in the commit
2. String _comment: A string storing the comment for that specific commit
3. String _timestamp: A string representing the timestamp of the commit
(could consider making a Timestamp object)
4. String _ID: A string representing the ID of the commit
5. HashMap commits: A static HashMap that holds all of the commits ever done, hashed by ID

###Main
This class runs Gitlet through directories, files, and commits

####Fields
1. Scanner _running: A scanner for the Gitlet
2. HashMap _connect: A HashMap connecting all of the files and directory names to their respective directory/file pathway
3. Directory repo: the main directory for all of the other files, master directory

## Algorithms

###Directory
1. Directory(String name): The class constructor. Create a new directory with a pathway and initiate all instance variables to empty, and establishes the name instance variable and pathway instance variable.
2. save(String name): Saves the status of the file or directory as it is, and creates a new file with the given name if one doesn't already exist.
3. remove(String name): removes the file or directory
4. commit(String comment): commits anything that has been added, and does this through creating a new instance of commit
5. log(): prints out every commit done from the instance variable _commits
6. add(String name): adds a file/s or directory to be commited

###Commit
1. Commit(String comment, ArrayList files): updates the files and directories given, creates a new commit with an ID and timestamp
2. find(String ID): searches all the commits through the HashMap commits and returns the ones with the applicable ID

###Main
1. Main(): creates a new master directory and allows for the user to type in commands, master directory is rep
2. log(): prints out all of the commits by printing the Commit.commits
3. init(): makes a new directory and an initial commit within that directory

## Persistence
By starting up the program, it will create a repo directory and an initial commit through the command init(). Based on the command run, structure of files will be saved.
1. add, to add the files to the next commit
2. init, to remember the new directory created
3. commit, to save the changes for the added files and modify the existing files
4. remove, to remove a file and save the removal
5. merge, to save the changes from both branches 

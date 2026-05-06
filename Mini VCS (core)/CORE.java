
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.security.NoSuchAlgorithmException;
// Handles all repository operations
// CLI entry point

public class core {

    public static void main(String[] args) {

        Repository core = new Repository();

        // No command provided
        if (args.length == 0) {
            System.out.println("No Command yet");
            return;
        }

        String command = args[0];

        // Command parser
        switch (command) {

            case "init":
                core.init();
                break;

            case "commit":
                if (args.length < 2) {
                    System.out.println("Commit message required");
                    return;
                }
                core.commit(args[1]);
                break;

            case "status":
                core.status();
                break;

            case "log":
                core.log();
                break;

            case "branch":
                core.branch(args[1]);
                break;

            case "branches":
                core.branches();
                break;

            case "checkout":
                core.checkout(args[1]);
                break;

            case "restore":
                if (args.length < 2) {
                    System.out.println("Commit ID required");
                    return;
                }
                core.restore(args[1]);
                break;

            case "help":

                System.out.println("""
=================================================
            CORE - MINI VERSION CONTROL
=================================================

>> INIT

Description:
    Initialize a new repository in current directory

Syntax:
    core init


>> COMMIT

Description:
    Create a new commit snapshot

Syntax:
    core commit "<message>"

>> STATUS

Description:
    Show repository status

Syntax:
    core status

>> LOG

Description:
    Display commit history

Syntax:
    core log


>> BRANCH

Description:
    Create a new branch

Syntax:
    core branch <branch-name>


>> BRANCHES

Description:
    List all available branches

Syntax:
    core branches


>> CHECKOUT

Description:
    Switch active branch

Syntax:
    core checkout <branch-name>


>> RESTORE

Description:
    Restore files from a specific commit

Syntax:
    core restore <commit-id>


>> HELP

Description:
    Show help menu

Syntax:
    core help

=================================================
""");

                break;
            default:
                System.out.println("Unknown Command.try-> core help");
        }
    }
}

class Repository {

    // Core repository structure
    final private File repoRoot;     // .core directory
    final private File objectDir;    // stores objects (commits + file contents)
    final private File refDir;     // stores branch pointers
    final private File mainFile;     // default branch file

    // Initialize file paths
    Repository() {
        repoRoot = new File(System.getProperty("user.dir"), ".core");
        objectDir = new File(repoRoot, "objects");
        refDir = new File(repoRoot, "refs");
        mainFile = new File(refDir, "main");
    }

    // Initializes repository structure
    void init() {
        if (repoRoot.exists()) {
            System.out.println(".core already exists");
            return;
        }

        repoRoot.mkdirs();
        objectDir.mkdirs();
        refDir.mkdirs();

        try {
            Path path = repoRoot.toPath();
            DosFileAttributeView view
                    = Files.getFileAttributeView(path, DosFileAttributeView.class);

            if (view != null) {
                view.setHidden(true);
            }
        } catch (IOException e) {
            System.out.println("Could not hide directory");
        }

        createFileContent(repoRoot.getAbsolutePath(), "HEAD", "main");

        try {
            mainFile.createNewFile();
        } catch (IOException e) {
            System.out.println(e);
        }

        System.out.println("\nInitialized empty repository in "
                + repoRoot.getAbsolutePath() + "\n");
    }

    // Creates a new commit
    public void commit(String message) {

        // Ensure repository exists
        if (!repoRoot.exists()) {
            System.out.println("No .core found");
            return;
        }

        // Get all files in working directory
        File[] files = new File(System.getProperty("user.dir")).listFiles();

        // HEAD contains current branch name
        File headFile = new File(repoRoot, "HEAD");

        if (!headFile.exists()) {
            System.out.println("No HEAD File");
            return;
        }

        // Read current branch
        String branch = null;
        try {
            branch = new String(java.nio.file.Files.readAllBytes(headFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        // Branch file stores latest commit hash
        File branchFile = new File(refDir, branch);

        // Default parent is null (first commit)
        String parentid = "null";

        try {
            String content = new String(java.nio.file.Files.readAllBytes(branchFile.toPath())).trim();

            // If branch already has commits, use last commit as parent
            if (!content.isEmpty()) {
                parentid = content;
            }
        } catch (IOException e) {
            System.err.println(e);
        }

        // Timestamp for commit
        String currenttime = java.time.LocalDateTime.now()
                .toString();

        // -------- FILE TRACKING --------
        // Stores mapping of filename -> content hash
        StringBuilder fileMap = new StringBuilder();
        if (fileMap.toString().equals("files:\n")) {
            System.out.println("Nothing to commit");
            return;
        }
        fileMap.append("files:\n");

        File workingDir = new File(System.getProperty("user.dir"));
        files = workingDir.listFiles();

        if (files != null) {
            for (File file : files) {

                // skip unwanted files
                if (!file.isFile()) {
                    continue;
                }

                String name = file.getName();

                // ignore .core folder, compiled files, hidden/system junk
                if (name.equals(".core")
                        || name.endsWith(".class")
                        || name.startsWith(".")) {
                    continue;
                }

                try {
                    byte[] data = java.nio.file.Files.readAllBytes(file.toPath());

                    // skip empty files (important fix)
                    if (data.length == 0) {
                        continue;
                    }

                    String content = new String(data);

                    String hash = HashUtil.getHex(HashUtil.getSHA(content));

                    File obj = new File(objectDir, hash);

                    // store only if not exists
                    if (!obj.exists()) {
                        try (FileWriter fw = new FileWriter(obj)) {
                            fw.write(content);
                        }
                    }

                    // add to commit snapshot
                    fileMap.append(name)
                            .append(" ")
                            .append(hash)
                            .append("\n");

                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        }

        // Build commit content (metadata + snapshot)
        String CommitContent
                = "parent:" + parentid
                + "\nauthor:Utkarsh"
                + "\ndate:" + currenttime
                + "\nmessage:" + message
                + "\n" + fileMap.toString();

        // Generate commit ID from content
        String commitId = null;
        try {
            commitId = HashUtil.getHex(HashUtil.getSHA(CommitContent));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        if (commitId == null) {
            return;
        }

        // Store commit object
        createFileContent(objectDir.getAbsolutePath(), commitId, CommitContent);

        // Update branch pointer to new commit
        try (FileWriter fw = new FileWriter(branchFile.getAbsoluteFile())) {
            fw.write(commitId);
        } catch (IOException e) {
            System.err.println(e);
        }

        // Print commit summary
        System.out.println("[" + branch + " " + commitId.substring(0, 6) + "] " + message);
    }

    public void status() {

        if (!repoRoot.exists()) {
            System.out.println("No .core found");
            return;
        }

        File head = new File(repoRoot, "HEAD");

        String currentBranch = "";

        try {
            currentBranch = new String(
                    java.nio.file.Files.readAllBytes(head.toPath())
            ).trim();

        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        File branchFile = new File(refDir, currentBranch);

        String latestCommit = "";

        try {
            latestCommit = new String(
                    java.nio.file.Files.readAllBytes(branchFile.toPath())
            ).trim();

        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        int objectCount = 0;

        File[] objects = objectDir.listFiles();

        if (objects != null) {
            objectCount = objects.length;
        }

        System.out.println("On branch " + currentBranch);

        if (!latestCommit.isEmpty()) {
            System.out.println("Latest commit: "
                    + latestCommit.substring(0, 6));
        } else {
            System.out.println("No commits yet");
        }

        System.out.println("Stored objects: " + objectCount);
    }

    // Displays commit history
    void log() {

        if (!repoRoot.exists()) {
            return;
        }

        File headFile = new File(repoRoot, "HEAD");

        // Get current branch
        String branch = null;
        try {
            branch = new String(java.nio.file.Files.readAllBytes(headFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        File branchFile = new File(refDir, branch);

        // Get latest commit
        String currentCommit = null;
        try {
            currentCommit = new String(java.nio.file.Files.readAllBytes(branchFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        System.out.println("\nOn branch " + branch + "\n");

        // Traverse commits backwards using parent links
        while (currentCommit != null && !currentCommit.equals("null")) {

            File commitFile = new File(objectDir, currentCommit);

            if (!commitFile.exists()) {
                System.out.println("Missing commit object: " + currentCommit);
                return;
            }

            System.out.println("commit " + currentCommit.substring(0, 6));

            String parentHash = "null";

            try (BufferedReader reader = new BufferedReader(new FileReader(commitFile))) {

                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("author:")) {
                        System.out.println("Author: " + line.substring(7));
                    } else if (line.startsWith("date:")) {
                        System.out.println("Date:   " + line.substring(5));
                    } else if (line.startsWith("message:")) {
                        System.out.println("\n    " + line.substring(8));
                    }

                    // First line contains parent reference
                    if (firstLine) {
                        int index = line.indexOf(':');
                        parentHash = line.substring(index + 1);
                        firstLine = false;
                    }
                }

            } catch (IOException e) {
                System.err.println(e);
                return;
            }

            System.out.println();

            // Move to parent commit
            currentCommit = parentHash;
        }
    }

    // Creates a new branch
    public void branch(String branchName) {

        if (branchName == null || branchName.trim().isEmpty()) {
            System.out.println("Invalid branch name");
            return;
        }

        if (!repoRoot.exists()) {
            System.out.println("No Core Exist");
            return;
        }

        // Get current branch
        File head = new File(repoRoot, "HEAD");
        String currbranch = null;

        try {
            currbranch = new String(java.nio.file.Files.readAllBytes(head.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
        }

        // Get latest commit of current branch
        File currentBranchFile = new File(refDir, currbranch);
        String currH = null;

        try {
            currH = new String(java.nio.file.Files.readAllBytes(currentBranchFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
        }

        if (currH.isEmpty()) {
            System.out.println("No Commits yet");
            return;
        }

        // Create new branch pointer
        File newBranch = new File(refDir, branchName);

        if (!newBranch.exists()) {
            createFileContent(refDir.getAbsolutePath(), branchName, currH);
            System.out.println("Branch '" + branchName + "' created");
        } else {
            System.out.println(branchName + " already exists");
        }
    }

    public void branches() {

        if (!repoRoot.exists()) {
            System.out.println("No .core found");
            return;
        }

        File head = new File(repoRoot, "HEAD");

        String currentBranch = "";

        try {
            currentBranch = new String(
                    java.nio.file.Files.readAllBytes(head.toPath())
            ).trim();

        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        File[] branchFiles = refDir.listFiles();

        if (branchFiles == null) {
            return;
        }

        for (File branch : branchFiles) {

            if (branch.getName().equals(currentBranch)) {
                System.out.println("* " + branch.getName());
            } else {
                System.out.println("  " + branch.getName());
            }
        }
    }

    public void checkout(String branchName) {

        if (branchName == null || branchName.trim().isEmpty()) {
            System.out.println("Invalid branch name");
            return;
        }

        if (!repoRoot.exists()) {
            System.out.println("No Core exists");
            return;
        }

        File branch = new File(refDir, branchName);

        if (!branch.exists()) {
            System.out.println(branchName + " does not exist");
            return;
        }

        // update HEAD to selected branch
        File head = new File(repoRoot, "HEAD");

        try (FileWriter fw = new FileWriter(head)) {
            fw.write(branchName);
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        // read latest commit of target branch
        String commitId = null;
        try {
            commitId = new String(java.nio.file.Files.readAllBytes(branch.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        // if branch has no commits yet, just switch HEAD
        if (commitId.isEmpty()) {
            System.out.println("Switched to branch '" + branchName + "'");
            return;
        }

        // restore working directory to target branch state
        restore(commitId);

        System.out.println("Switched to branch '" + branchName + "'");
    }

    public void restore(String commitId) {

        // check repo exists
        if (!repoRoot.exists()) {
            System.out.println("No .core found");
            return;
        }

        File commitFile = null;

        // try exact match
        File exact = new File(objectDir, commitId);
        if (exact.exists()) {
            commitFile = exact;
        } else {
            // try prefix match (short hash support)
            File[] all = objectDir.listFiles();
            if (all != null) {
                for (File f : all) {
                    if (f.getName().startsWith(commitId)) {
                        commitFile = f;
                        break;
                    }
                }
            }
        }

        if (commitFile == null) {
            System.out.println("Commit not found");
            return;
        }

        // clean working directory (except .core)
        File workingDir = new File(System.getProperty("user.dir"));
        File[] files = workingDir.listFiles();

        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
    f.delete();
}
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(commitFile))) {

            String line;
            boolean fileSection = false;

            while ((line = reader.readLine()) != null) {

                // start reading file mappings
                if (line.equals("files:")) {
                    fileSection = true;
                    continue;
                }

                if (!fileSection) {
                    continue;
                }

                // format: filename hash
                String[] parts = line.split(" ");
                if (parts.length != 2) {
                    continue;
                }

                String fileName = parts[0];
                String hash = parts[1];

                File objFile = new File(objectDir, hash);

                if (!objFile.exists()) {
                    System.out.println("Missing object: " + hash);
                    continue;
                }

                // read stored content
                String content = new String(java.nio.file.Files.readAllBytes(objFile.toPath()));

                // recreate file
                File outFile = new File(workingDir, fileName);

                try (FileWriter fw = new FileWriter(outFile)) {
                    fw.write(content);
                }
            }

            System.out.println("HEAD restored to commit "
                    + commitFile.getName().substring(0, 6));

        } catch (IOException e) {
            System.err.println(e);
        }
    }

    // Utility to create a file with content
    void createFileContent(String FP, String FN, String Content) {
        File file = new File(FP, FN);
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(Content);
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}

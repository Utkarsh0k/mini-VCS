
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

            case "branch":
                core.branch(args[1]);
                break;

            case "log":
                core.log();
                break;

            case "checkout":
                core.checkout(args[1]);
                break;

            case "help":
                System.out.println("Usable Commands:");
                System.out.println("""
                                        init : initializes repository
                                        commit : Create new Commit
                                        log : show commit history
                                        branch : create new branch
                                        checkout : Switch Branch""");
                break;

            case "restore":
                if (args.length < 2) {
                    System.out.println("Commit ID required");
                    return;
                }
                core.restore(args[1]);
                break;

            default:
                System.out.println("Unknown Command");
        }
    }
}

class Repository {

    // Core repository structure
    final private File repoRoot;     // .core directory
    final private File objectDir;    // stores objects (commits + file contents)
    final private File refdirec;     // stores branch pointers
    final private File mainFile;     // default branch file

    // Initialize file paths
    Repository() {
        repoRoot = new File(System.getProperty("user.dir"), ".core");
        objectDir = new File(repoRoot, "objects");
        refdirec = new File(repoRoot, "refs");
        mainFile = new File(refdirec, "main");
    }

    // Initializes repository structure
    void init() {
        if (repoRoot.exists()) {
            System.out.println(".core already exists");
            return;
        }

        repoRoot.mkdirs();
        objectDir.mkdirs();
        refdirec.mkdirs();

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

        FileCreateContent(repoRoot.getAbsolutePath(), "HEAD", "main");

        try {
            mainFile.createNewFile();
        } catch (IOException e) {
            System.out.println(e);
        }

        System.out.println("\nInitialized empty repository in "
                + repoRoot.getAbsolutePath() + "\n");
    }

    // Utility to create a file with content
    void FileCreateContent(String FP, String FN, String Content) {
        File file = new File(FP, FN);
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(Content);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    // Creates a new commit
    public void commit(String Message) {

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
        File branchFile = new File(refdirec, branch);

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
        long currenttime = System.currentTimeMillis();

        // -------- FILE TRACKING --------
        // Stores mapping of filename -> content hash
        StringBuilder fileMap = new StringBuilder();
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
                + "\nmessage:" + Message
                + "\n" + fileMap.toString();

        // Generate commit ID from content
        String CommitID = null;
        try {
            CommitID = HashUtil.getHex(HashUtil.getSHA(CommitContent));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        if (CommitID == null) {
            return;
        }

        // Store commit object
        FileCreateContent(objectDir.getAbsolutePath(), CommitID, CommitContent);

        // Update branch pointer to new commit
        try (FileWriter fw = new FileWriter(branchFile.getAbsoluteFile())) {
            fw.write(CommitID);
        } catch (IOException e) {
            System.err.println(e);
        }

        // Print commit summary
        System.out.println("[" + branch + " " + CommitID.substring(0, 6) + "] " + Message);
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

        File branchFile = new File(refdirec, branch);

        // Get latest commit
        String currentCommit = null;
        try {
            currentCommit = new String(java.nio.file.Files.readAllBytes(branchFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        System.out.println("\nLog Chain\n");

        // Traverse commits backwards using parent links
        while (currentCommit != null && !currentCommit.equals("null")) {

            File commitFile = new File(objectDir, currentCommit);

            if (!commitFile.exists()) {
                System.out.println("Missing commit object: " + currentCommit);
                return;
            }

            System.out.println("commit " + currentCommit);

            String parentHash = "null";

            try (BufferedReader reader = new BufferedReader(new FileReader(commitFile))) {

                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {

                    System.out.println(line);

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
    public void branch(String Branch_name) {

        if (Branch_name == null || Branch_name.trim().isEmpty()) {
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
        File currentBranchFile = new File(refdirec, currbranch);
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
        File newBranch = new File(refdirec, Branch_name);

        if (!newBranch.exists()) {
            FileCreateContent(refdirec.getAbsolutePath(), Branch_name, currH);
        } else {
            System.out.println(Branch_name + " already exists");
        }
    }

    // Switches active branch
    public void checkout(String Branch_name) {

        if (Branch_name == null || Branch_name.trim().isEmpty()) {
            System.out.println("Invalid branch name");
            return;
        }

        if (!repoRoot.exists()) {
            System.out.println("No Core exists");
            return;
        }

        File branch = new File(refdirec, Branch_name);

        if (!branch.exists()) {
            System.out.println(branch + " does not exist");
            return;
        }

        // Update HEAD to new branch
        File head = new File(repoRoot, "HEAD");

        try (FileWriter fw = new FileWriter(head.getAbsolutePath())) {
            fw.write(Branch_name);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void restore(String commitID) {

        // check repo exists
        if (!repoRoot.exists()) {
            System.out.println("No .core found");
            return;
        }

        File commitFile = null;

        // try exact match
        File exact = new File(objectDir, commitID);
        if (exact.exists()) {
            commitFile = exact;
        } else {
            // try prefix match (short hash support)
            File[] all = objectDir.listFiles();
            if (all != null) {
                for (File f : all) {
                    if (f.getName().startsWith(commitID)) {
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
                if (f.isFile() && !f.getName().equals(".core")) {
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

            System.out.println("Restored to commit " + commitFile.getName().substring(0, 6));

        } catch (IOException e) {
            System.err.println(e);
        }
    }
}

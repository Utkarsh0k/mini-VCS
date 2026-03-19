
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

class Repository {

    private File repoRoot;
    private File objectDir;
    private File refdirec;
    private File mainFile;

    Repository() {
        repoRoot = new File(System.getProperty("user.dir"), ".core");
        objectDir = new File(repoRoot, "objects");
        refdirec = new File(repoRoot, "refs");
        mainFile = new File(refdirec, "main");
    }

    void init() {
        if (repoRoot.exists()) {
            System.out.println(".core Already exists");
        } else {
            repoRoot.mkdirs();
            objectDir.mkdirs();
            refdirec.mkdirs();
            FileCreateContent(repoRoot.getAbsolutePath(), "HEAD", "main");
            try {
                mainFile.createNewFile();
            } catch (IOException e) {
                System.out.println(e);
            }
            System.out.println("\nInitialized empty Git repository in "+repoRoot.getAbsolutePath()+"\n");
            
        }
    }

    void FileCreateContent(String FP, String FN, String Content) {
        File head = new File(FP, FN);
        try (FileWriter fw = new FileWriter(head)) {
            fw.write(Content);
        } catch (IOException e) {
            System.err.println(e);
        }

    }

    public void commit(String Message) {
        if (!repoRoot.exists()) {
            System.out.println("No .core found");
            return;
        }
        File headFile = new File(repoRoot, "HEAD");

        if (!headFile.exists()) {
            System.out.println("No HEAD File");
            return;
        }

        String branch = null;
        try {
            branch = new String(java.nio.file.Files.readAllBytes(headFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }
        File branchFile = new File(refdirec, branch);
        String parentid = "null";

        try {
            String Content = new String(java.nio.file.Files.readAllBytes(branchFile.toPath())).trim();
            if (!Content.isEmpty()) {
                parentid = Content;
            }

        } catch (IOException e) {
            System.err.println(e);
        }
        long currenttime = System.currentTimeMillis();

        String CommitContent = "parent:" + parentid + "\nauthor:Utkarsh\ndate:" + currenttime + "\nmessage:" + Message;
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
        FileCreateContent(objectDir.getAbsolutePath(), CommitID, CommitContent);
        try (FileWriter fw = new FileWriter(branchFile.getAbsoluteFile())) {
            fw.write(CommitID);
        } catch (IOException e) {
            System.err.println(e);
        }
        System.out.println("["+branch+" "+CommitID.substring(0,6)+"]"+ " "+ Message);
    }

    void log() {

        if (!repoRoot.exists()) {
            return;
        }

        File headFile = new File(repoRoot, "HEAD");

        String branch = null;
        try {
            branch = new String(java.nio.file.Files.readAllBytes(headFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        File branchFile = new File(refdirec, branch);

        String currentCommit = null;
        try {
            currentCommit = new String(java.nio.file.Files.readAllBytes(branchFile.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        System.out.println("\nLog Chain\n");

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

            currentCommit = parentHash;
        }

    }

    public void branch(String Branch_name) {
        if (Branch_name == null || Branch_name.trim().isEmpty()) {
            System.out.println("Invalid branch name");
            return;
        }
        if (!repoRoot.exists()) {
            System.out.println("No Core Exist");
            return;
        }
        File head = new File(repoRoot, "HEAD");
        String currbranch = null;
        try {
            currbranch = new String(java.nio.file.Files.readAllBytes(head.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
        }
        File m = new File(refdirec, currbranch);

        String currH = null;
        try {
            currH = new String(java.nio.file.Files.readAllBytes(m.toPath())).trim();
        } catch (IOException e) {
            System.err.println(e);
        }
        if (currH.isEmpty()) {
            System.out.println("No Commits yet");
            return;
        }
        File check = new File(refdirec, Branch_name);
        if (!check.exists()) {
            FileCreateContent(refdirec.getAbsolutePath(), Branch_name, currH);
        } else {
            System.out.println(Branch_name + " already exists");
        }
    }

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
        File head = new File(repoRoot, "HEAD");
        try (FileWriter fw = new FileWriter(head.getAbsolutePath())) {
            fw.write(Branch_name);
        } catch (IOException e) {
            System.err.println(e);
        }

    }
}

public class core {

    public static void main(String[] args) {
        Repository core = new Repository();
        if (args.length == 0) {
            System.out.println("No Command yet");
            return;
        }
        String command = args[0];
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
                System.out.println(
                        "init : initializes repository\n"
                        + "commit : Create new Commit\n"
                        + "log : show commit history\n"
                        + "branch : create new branch\n"
                        + "checkout : Switch Branch"
                );
                break;
            default:
                System.out.println("Unknown Command");

        }
    }

}

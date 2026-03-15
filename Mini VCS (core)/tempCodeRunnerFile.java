
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class Repository {

    private File Reporoot;
    private File Objectdirec;
    private File refdirec;
    private File mainFile;

    Repository() {
        Reporoot = new File(System.getProperty("user.dir"), ".core");
        Objectdirec = new File(Reporoot, "objects");
        refdirec = new File(Reporoot, "refs");
        mainFile = new File(refdirec, "main");
    }

    void init() {
        if (Reporoot.exists()) {
            System.out.println(".core Already exists");
        } else {
            Reporoot.mkdirs();
            Objectdirec.mkdirs();
            refdirec.mkdirs();

            FileCreateContent(Reporoot.getAbsolutePath(), "HEAD", "main");
            try {
                mainFile.createNewFile();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    void FileCreate(String FP, String FN) {
        try {
            File head = new File(FP, FN);
            if (head.createNewFile()) {
                System.out.println(FN + " File Created ");
            } else {
                System.out.println(FN + " Already exists");
            }

        } catch (IOException e) {
            System.err.println(e);
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
        if (!Reporoot.exists()) {
            System.out.println("No .core found");
            return;
        }
        String branchName = null;

        try {
            branchName = new String(java.nio.file.Files.readAllBytes(headFile.toPath())).trim();
        } catch (IOException e) {
            System.out.println("Cannot read HEAD");
            return;
        }

    }

}

public class initmaker {

    public static void main(String[] args) {
        Repository rep = new Repository();
        rep.init();
    }

}

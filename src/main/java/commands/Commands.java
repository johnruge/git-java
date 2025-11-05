package commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Commands {

    public static void init() {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
            head.createNewFile();
            Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
            System.out.println("Initialized git directory");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void catFile(String sha) throws IOException {
        final String blobFolder = sha.substring(0, 2);
        final String blobFile = sha.substring(2);
        final String temp = ".git/objects/" + blobFolder + "/" + blobFile;
        final Path path = Path.of(temp);

        byte[] bytes = Files.readAllBytes(path);

        int idx = -1;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                idx = i;
                break;
            }
        }

        if (idx != -1 && idx < bytes.length) {
            byte[] afterNull = Arrays.copyOfRange(bytes, idx + 1, bytes.length);
            String result = new String(afterNull);
            System.out.println(result);
        }
    }
}

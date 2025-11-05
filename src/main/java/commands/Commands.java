package commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

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
        final Path path = Path.of(".git/objects/" + sha.substring(0, 2)
                         + "/" + sha.substring(2));

        try (InputStream fileStream = Files.newInputStream(path);
            InflaterInputStream inflater = new InflaterInputStream(fileStream)) {

            // read decompressed bytes
            byte[] decompressed = inflater.readAllBytes();
            String content = new String(decompressed);

            // find where the null terminator
            int nullIndex = content.indexOf('\0');
            if (nullIndex != -1 && nullIndex + 1 < content.length()) {
                String blobData = content.substring(nullIndex + 1);
                System.out.print(blobData);
            }
        }
    }
}

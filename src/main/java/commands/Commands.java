package commands;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.swing.text.StyledEditorKit;

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

    public static void hashObject(String path) throws Exception {
        // read from file and store blob content in bytes
        byte[] content = Files.readAllBytes(Path.of(path));
        byte[] header = ("blob " + content.length + '\0').getBytes();
        byte[] blobContent = new byte[content.length + header.length];

        System.arraycopy(header, 0, blobContent, 0, header.length);
        System.arraycopy(content, 0, blobContent, header.length, content.length);

        // generate sha-1 and create file path
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest(blobContent);
        String hash = bytesToHex(digest);
        String gitPath = ".git/objects/" + hash.substring(0, 2);
        String fileName = hash.substring(2);

        Files.createDirectories(Path.of(gitPath));
        Path objectPath = Path.of(gitPath, fileName);

        // zlib encode and write to file
        try (OutputStream fileOut = Files.newOutputStream(objectPath);
            DeflaterOutputStream zlibOut = new DeflaterOutputStream(fileOut)) {
            zlibOut.write(blobContent);
        }

        System.out.println(hash);
    }

    // This is a helper function that doesn't print anything to output
    // it will be used to make a Tree object
    public static String hashObjectTree(Path cwd) throws Exception {
        // read from file and store blob content in bytes
        byte[] content = Files.readAllBytes(cwd);
        byte[] header = ("blob " + content.length + '\0').getBytes();
        byte[] blobContent = new byte[content.length + header.length];

        System.arraycopy(header, 0, blobContent, 0, header.length);
        System.arraycopy(content, 0, blobContent, header.length, content.length);

        // generate sha-1 and create file path
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest(blobContent);
        String hash = bytesToHex(digest);
        String gitPath = ".git/objects/" + hash.substring(0, 2);
        String fileName = hash.substring(2);

        Files.createDirectories(Path.of(gitPath));
        Path objectPath = Path.of(gitPath, fileName);

        // zlib encode and write to file
        try (OutputStream fileOut = Files.newOutputStream(objectPath);
            DeflaterOutputStream zlibOut = new DeflaterOutputStream(fileOut)) {
            zlibOut.write(blobContent);
        }
        return hash;
    }

    public static void lsTree(String sha) throws Exception{
        final Path path = Path.of(".git/objects/" + sha.substring(0, 2)
                         + "/" + sha.substring(2));

        try (InputStream fileStream = Files.newInputStream(path);
            InflaterInputStream inflater = new InflaterInputStream(fileStream)) {

            // read decompressed bytes
            byte[] decompressed = inflater.readAllBytes();
            String content = new String(decompressed);
            String [] parts = content.split(" ");

            // read the contents of the trees and strip away the other information
            // to stay consistent with the --name-only flag
            for (int i = 0; i < parts.length; i++) {
                if (i > 1) {
                    String curr = parts[i];
                    for (int j = 0; j < curr.length(); j++) {
                        if (curr.charAt(j) == '\0') {
                            System.out.println(curr.substring(0, j));
                            break;
                        }
                    }
                }
            }

        }

    }

    public static String writeTree(Path cwd) throws Exception {
        File[] entries = cwd.toFile().listFiles();
        Arrays.sort(entries, (a, b) -> a.getName().compareTo(b.getName()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (File f : entries) {
            if (f.getName().equals(".git")) {
                continue;
            }

            if (f.isFile()) {
                //create the blob object
                String sha = hashObjectTree(f.toPath());

                out.write(("100644 " + f.getName()).getBytes());
                out.write(0); // null byte
                out.write(hexToBytes(sha));   //turn hex to bytes ans write the sha
            } else { //f is a dir
                String sha = writeTree(f.toPath());

                //do the same for the dir after the recursive call return the sha
                out.write(("40000 " + f.getName()).getBytes());
                out.write(0);
                out.write(hexToBytes(sha));
            }
        }

        // build the header and content
        byte[] content = out.toByteArray();
        byte[] header = ("tree " + content.length + "\0").getBytes();
        byte[] store = new byte[header.length + content.length];
        System.arraycopy(header, 0, store, 0, header.length);
        System.arraycopy(content, 0, store, header.length, content.length);

        // hash
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest(store);
        String sha = bytesToHex(digest);

        // write object to .git/objects
        String gitPath = ".git/objects/" + sha.substring(0, 2);
        String fileName = sha.substring(2);

        Files.createDirectories(Path.of(gitPath));
        Path objectPath = Path.of(gitPath, fileName);

        try (OutputStream fileOut = Files.newOutputStream(objectPath);
            DeflaterOutputStream zlibOut = new DeflaterOutputStream(fileOut)) {
            zlibOut.write(store);
        }

        return sha;
    }

    public static void commitTree(String treeSha, String parentSha, String message)
    throws Exception {
        // write bytes for all the content of the commit
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("tree " + treeSha).getBytes());
        out.write(("parent " + parentSha).getBytes());
        //use placeholders for author and committer creds for this challenge
        out.write(("author <name> <email> timestamp").getBytes());
        out.write(("committer <name> <email> timestamp").getBytes());
        //write the commit message
        out.write((message).getBytes());

        // combine header and content and tranform to byte array for hashingx
        byte[] outByteArray = out.toByteArray();
        byte[] header = ("commit " + outByteArray.length + '\0').getBytes();
        byte[] store = new byte[header.length + outByteArray.length];

        System.arraycopy(header, 0, store, 0, header.length);
        System.arraycopy(outByteArray, 0, store, header.length, outByteArray.length);

        // generate sha-1 and create file path
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest(store);
        String hash = bytesToHex(digest);
        String gitPath = ".git/objects/" + hash.substring(0, 2);
        String fileName = hash.substring(2);

        Files.createDirectories(Path.of(gitPath));
        Path objectPath = Path.of(gitPath, fileName);

        // zlib encode and write to file
        try (OutputStream fileOut = Files.newOutputStream(objectPath);
            DeflaterOutputStream zlibOut = new DeflaterOutputStream(fileOut)) {
            zlibOut.write(store);
        }

        System.out.println(hash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[20];
        for (int i = 0; i < 40; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }
}

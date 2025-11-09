import java.io.IOException;
import java.nio.file.Path;
import commands.Commands;

public class Main {
    public static void main(String[] args) {
        final String command = args[0];

        switch (command) {
            case "init" -> Commands.init();
            case "cat-file" -> {
              final String sha = args[2];
              try {
                Commands.catFile(sha);
              } catch (IOException e) {
                System.err.println("fatal: could not read object " + sha);
              }
            }
            case "hash-object" -> {
              final String filePath = args[2];
              try {
                Commands.hashObject(filePath);
              } catch (Exception e) {
                System.err.println("fatal: could not hash file " + filePath + ": " + e.getMessage());
              }
            }
            case "ls-tree" -> {
              final String sha = args[2];
              try {
                Commands.lsTree(sha);
              } catch (Exception e) {
                System.err.println("Fatal: Couldn't read object " + sha);;
              }
            }
            case "write-tree" -> {
              Path cwd = Path.of(".");
              try {
                String sha = Commands.writeTree(cwd);
                System.out.println(sha);
              } catch (Exception e) {
                System.err.println("Fatal: Couldn't write tree");
              }
            }
            case "commit-tree" -> {
              String treeSha = args[1];
              String parentSha = args[3];
              String message = args[5];
              try {
                Commands.commitTree(treeSha, parentSha, message);
              } catch (Exception e) {
                System.out.println("Fatal: Couldn't commit tree");
              }
            }
            default -> System.out.println("Unknown command: " + command);
        }
    }
}

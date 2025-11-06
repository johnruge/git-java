import java.io.IOException;

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
            default -> System.out.println("Unknown command: " + command);
        }
    }
}

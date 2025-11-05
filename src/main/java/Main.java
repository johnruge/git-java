import commands.Commands;

public class Main {
    public static void main(String[] args) {
        final String command = args[0];

        switch (command) {
            case "init" -> Commands.init();
            case "cat-file" -> {
              final String sha = args[2];
              Commands.catFile(sha);
            }
            default -> System.out.println("Unknown command: " + command);
        }
    }
}

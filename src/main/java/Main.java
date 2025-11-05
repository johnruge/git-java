import commands.Commands;

public class Main {
    public static void main(String[] args) {
        final String command = args[0];

        switch (command) {
            case "init" -> Commands.init();
            default -> System.out.println("Unknown command: " + command);
        }
    }
}

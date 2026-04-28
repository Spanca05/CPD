package protocol;

public class CommandParser {
    
    public static Command parse(String line) {
        if (line.equals("/help")) {
            return new Command(CommandType.HELP, null);
        }

        if (line.equals("/rooms")) {
            return new Command(CommandType.ROOMS, null);
        }

        if (line.equals("/join") || line.startsWith("/join ")) {
            String roomName = line.substring("/join".length()).trim();
            return new Command(CommandType.JOIN, roomName);
        }

        if (line.equals("/leave")) {
            return new Command(CommandType.LEAVE, null);
        }

        if (line.equals("/quit")) {
            return new Command(CommandType.QUIT, null);
        }

        if (line.startsWith("/")) {
            return new Command(CommandType.UNKNOWN, line);
        }

        return new Command(CommandType.MESSAGE, line);
    }
}

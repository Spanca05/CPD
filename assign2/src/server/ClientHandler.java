package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import auth.AuthService;
import auth.TokenService;
import ai.OllamaClient;
import model.TokenInfo;
import protocol.Command;
import protocol.CommandParser;
import protocol.CommandType;
import room.ChatRoom;
import room.RoomManager;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final RoomManager roomManager;
    private final AuthService authService;
    private ChatRoom currentRoom;
    private String username;
    private PrintWriter out;
    private boolean running = true;
    private boolean authenticated = false;
    private final TokenService tokenService;
    private String token;
    private final OllamaClient ollamaClient = OllamaClient.createDefault();
    private final Object outLock = new Object();

    public ClientHandler(Socket clientSocket, RoomManager roomManager, AuthService authService,
            TokenService tokenService) {
        this.clientSocket = clientSocket;
        this.roomManager = roomManager;
        this.authService = authService;
        this.tokenService = tokenService;
    }

    public void sendMessage(String message) {
        synchronized (outLock) {
            if (out != null) {
                out.println(message);
            }
        }
    }

    @Override
    public void run() {
        try (
                Socket socket = clientSocket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));) {
            out = new PrintWriter(socket.getOutputStream(), true);

            sendMessage("Welcome!");
            sendMessage("Use /register <username> <password> or /login <username> <password>.");
            sendMessage("Type /help to see all commands.");

            String line;

            while (running && (line = in.readLine()) != null) {
                Command cmd = CommandParser.parse(line);

                if (!authenticated &&
                        cmd.getType() != CommandType.LOGIN &&
                        cmd.getType() != CommandType.REGISTER &&
                        cmd.getType() != CommandType.RECONNECT &&
                        cmd.getType() != CommandType.HELP &&
                        cmd.getType() != CommandType.QUIT) {
                    sendMessage("You must login or register first.");
                    sendMessage("Use /login <username> <password> or /register <username> <password>.");
                    continue;
                }

                switch (cmd.getType()) {
                    case REGISTER:
                        handleRegister(cmd.getArgument());
                        break;

                    case LOGIN:
                        handleLogin(cmd.getArgument());
                        break;

                    case LOGOUT:
                        handleLogout();
                        break;

                    case HELP:
                        handleHelp();
                        break;

                    case ROOMS:
                        handleRooms();
                        break;

                    case JOIN:
                        handleJoin(cmd.getArgument());
                        break;

                    case AIROOM:
                        handleAiRoom(cmd.getArgument());
                        break;

                    case LEAVE:
                        handleLeave();
                        break;

                    case MESSAGE:
                        handleMessage(cmd.getArgument());
                        break;

                    case QUIT:
                        handleQuit();
                        break;

                    case RECONNECT:
                        handleReconnect(cmd.getArgument());
                        break;

                    case UNKNOWN:
                        sendMessage("Unknown command. Use /help");
                        break;

                    default:
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            if (currentRoom != null) {
                currentRoom.removeClient(this);
                currentRoom = null;
            }
        }
    }

    private void handleHelp() {
        sendMessage("All commands:");
        sendMessage("/register <username> <password> - create account");
        sendMessage("/login <username> <password>    - login");
        sendMessage("/logout                         - logout current user");
        sendMessage("/join <room>                    - join or create a room");
        sendMessage("/airoom <room> [prompt]         - create AI room with optional prompt and join");
        sendMessage("/leave                          - leave current room");
        sendMessage("/rooms                          - list available rooms");
        sendMessage("/quit                           - exit");
    }

    private void handleRooms() {
        sendMessage("Available rooms: " + roomManager.listRooms());
    }

    private void handleJoin(String roomName) {
        if (currentRoom != null) {
            sendMessage("You are already in room: " + currentRoom.getName());
            sendMessage("Use /leave before joining another room.");
            return;
        }

        if (roomName == null || roomName.isBlank()) {
            sendMessage("Usage: /join <room-name>");
            sendMessage("Available rooms: " + roomManager.listRooms());
            return;
        }

        currentRoom = roomManager.getOrCreateRoom(roomName);
        currentRoom.addClient(this);
        tokenService.updateRoom(token, currentRoom.getName());

        currentRoom.broadcastExcept(username + " entered the room.", this);
        sendMessage("Joined room: " + currentRoom.getName());
    }

    private void handleLeave() {
        if (currentRoom == null) {
            sendMessage("You are not in a room.");
            return;
        }

        String roomName = currentRoom.getName();
        currentRoom.removeClient(this);

        currentRoom.broadcastExcept(username + " left the room.", this);

        currentRoom = null;
        tokenService.updateRoom(token, null);
        sendMessage("You left room: " + roomName);
    }

    private void handleMessage(String message) {
        if (currentRoom == null) {
            sendMessage("You must join a room before sending messages. Use: /join <room-name>");
            sendMessage("Available rooms: " + roomManager.listRooms());
            return;
        }

        System.out
                .println("From room: " + currentRoom.getName() + ", user: " + username + ", sent message: " + message);
        currentRoom.broadcastExcept("//USER//"+ username + ": " + message, this);

        if (currentRoom.isAiRoom()) {
            try {
                StringBuilder ctx = new StringBuilder();

                String prompt = currentRoom.getPrompt();
                if (prompt != null && !prompt.isBlank()) {
                    ctx.append(prompt).append("\n");
                }

                for (String h : currentRoom.getHistory()) {
                    ctx.append(h).append("\n");
                }

                String context = ctx.toString();
                ChatRoom room = currentRoom; // Capture room reference for async callback

                boolean started = room.startAiRequestIfIdle(context);
                if (started) {
                    submitAiRequest(room, context);
                }
            } catch (Exception e) {
                currentRoom.broadcast("Bot: Sorry, AI unavailable.");
            }
        }
    }

    private void submitAiRequest(ChatRoom room, String context) {
        ollamaClient.generate(context).thenAccept(resp -> {
            try {
                String botResponse = extractResponseFromJson(resp);
                String botMsg = "Bot: " + botResponse;
                room.broadcast(botMsg);

                String next = room.finishAiRequestAndGetPending();
                if (next != null) {
                    submitAiRequest(room, next);
                }
            } catch (Exception e) {
                room.broadcast("Bot: Error parsing response.");
                room.finishAiRequestAndGetPending();
            }
        }).exceptionally(ex -> {
            room.broadcast("Bot: Sorry, AI unavailable.");
            room.finishAiRequestAndGetPending();
            return null;
        });
    }

    private String extractResponseFromJson(String json) throws Exception {
        if (json == null) throw new Exception("Empty JSON response");

        String[] candidates = {"response", "text", "output", "content"};

        for (String field : candidates) {
            String val = extractJsonStringField(json, field);
            if (val != null) {
                return val.trim();
            }
        }

        throw new Exception("No response field in JSON");
    }

    private String extractJsonStringField(String json, String field) {
        String key = '"' + field + '"';
        int idx = json.indexOf(key);
        if (idx < 0) return null;

        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return null;

        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;

        if (json.charAt(i) != '"') return null;

        int start = i + 1;
        int j = start;
        while (j < json.length()) {
            char c = json.charAt(j);
            if (c == '"') {
                int bs = 0;
                int k = j - 1;
                while (k >= start && json.charAt(k) == '\\') {
                    bs++;
                    k--;
                }

                if ((bs % 2) == 0) { // quote is not escaped
                    String raw = json.substring(start, j);
                    return unescapeJsonString(raw);
                }
            }
            j++;
        }

        return null;
    }

    private String unescapeJsonString(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private void handleQuit() {
        if (currentRoom != null) {
            currentRoom.removeClient(this);
            currentRoom = null;
        }

        sendMessage("Goodbye. See you next time :)");
        running = false;
    }

    private void handleRegister(String args) {
        if (authenticated) {
            sendMessage("You are already logged in as " + username + ". Use /logout first.");
            return;
        }

        if (args == null || args.isBlank()) {
            sendMessage("Usage: /register <username> <password>");
            return;
        }

        String[] parts = args.trim().split(" ", 2);

        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            sendMessage("Usage: /register <username> <password>");
            return;
        }

        String newUsername = parts[0].trim();
        String password = parts[1].trim();

        try {
            if (authService.register(newUsername, password)) {
                authenticated = true;
                username = newUsername;
                token = tokenService.createToken(username);
                sendMessage("Registration successful. Logged in as " + username + ".");
                sendMessage("TOKEN " + token);
                sendMessage("Use /join <room-name> to enter a room.");
            } else {
                sendMessage("Registration failed. User may already exist.");
            }
        } catch (Exception e) {
            sendMessage("Registration failed: " + e.getMessage());
        }
    }

    private void handleLogin(String args) {
        if (authenticated) {
            sendMessage("You are already logged in as " + username + ". Use /logout first.");
            return;
        }

        if (args == null || args.isBlank()) {
            sendMessage("Usage: /login <username> <password>");
            return;
        }

        String[] parts = args.trim().split(" ", 2);

        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            sendMessage("Usage: /login <username> <password>");
            return;
        }

        String loginUsername = parts[0].trim();
        String password = parts[1].trim();

        if (authService.login(loginUsername, password)) {
            authenticated = true;
            username = loginUsername;
            token = tokenService.createToken(username);
            sendMessage("TOKEN " + token);
            sendMessage("Login successful. Welcome, " + username + ".");
            sendMessage("Use /join <room-name> to enter a room.");
        } else {
            sendMessage("Login failed. Invalid username or password.");
        }
    }

    private void handleLogout() {
        if (!authenticated) {
            sendMessage("You are not logged in.");
            return;
        }

        if (currentRoom != null) {
            currentRoom.removeClient(this);
            currentRoom = null;
        }

        sendMessage("Logged out from " + username + ".");

        username = null;
        authenticated = false;

        sendMessage("Use /login <username> <password> or /register <username> <password>.");
    }

    private void handleReconnect(String token) {
        if (authenticated) {
            sendMessage("Already authenticated. Use /logout first.");
            return;
        }

        TokenInfo info = tokenService.validateToken(token);

        if (info == null) {
            sendMessage("Invalid or expired token");
            return;
        }

        authenticated = true;
        username = info.getUsername();

        sendMessage("Reconnected as " + username);

        if (info.getCurrentRoomName() != null) {
            currentRoom = roomManager.getOrCreateRoom(info.getCurrentRoomName());

            currentRoom.addClient(this);

            sendMessage("Rejoined room: " + currentRoom.getName() + ".");

            currentRoom.broadcastExcept(username + " reconnected.", this);
        }

    }

    private void handleAiRoom(String args) {
        if (currentRoom != null) {
            sendMessage("You are already in room: " + currentRoom.getName());
            sendMessage("Use /leave before joining another room.");
            return;
        }

        if (args == null || args.isBlank()) {
            sendMessage("Usage: /airoom <room-name> [prompt]");
            return;
        }

        String[] parts = args.trim().split(" ", 2);

        String roomName = parts[0].trim();
        if (roomName.isBlank()) {
            sendMessage("Usage: /airoom <room-name> [prompt]");
            return;
        }

        String prompt = null;
        if (parts.length >= 2 && !parts[1].isBlank()) {
            prompt = parts[1].trim();
        }

        ChatRoom room = roomManager.createAiRoom(roomName, prompt);

        currentRoom = room;

        currentRoom.addClient(this);

        tokenService.updateRoom(token, currentRoom.getName());

        currentRoom.broadcastExcept(username + " entered the room.", this);
        sendMessage("Created and joined AI room: " + currentRoom.getName());

        if (prompt != null && !prompt.isBlank()) {
            try {
                StringBuilder ctx = new StringBuilder();
                ctx.append(prompt).append("\n");

                for (String h : currentRoom.getHistory()) {
                    ctx.append(h).append("\n");
                }

                String context = ctx.toString();
                boolean started = room.startAiRequestIfIdle(context);
                if (started) {
                    submitAiRequest(room, context);
                }
            } catch (Exception e) {
                currentRoom.broadcast("Bot: Sorry, AI unavailable.");
            }
        }
    }
}

package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import auth.AuthService;
import auth.TokenService;
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

    public ClientHandler(Socket clientSocket, RoomManager roomManager, AuthService authService,
            TokenService tokenService) {
        this.clientSocket = clientSocket;
        this.roomManager = roomManager;
        this.authService = authService;
        this.tokenService = tokenService;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
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
        currentRoom.broadcastExcept(username + ": " + message, this);
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
}

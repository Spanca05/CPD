package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import protocol.Command;
import protocol.CommandParser;
import room.ChatRoom;
import room.RoomManager;

public class ClientHandler implements Runnable {
	private final Socket clientSocket;
    private final RoomManager roomManager;
    private ChatRoom currentRoom;
    private String username;
    private PrintWriter out;
    private boolean running = true;

	public ClientHandler(Socket clientSocket, RoomManager roomManager) {
		this.clientSocket = clientSocket;
        this.roomManager = roomManager;
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
                new InputStreamReader(socket.getInputStream())
            );
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);
            
            sendMessage("Enter your username: ");
            username = in.readLine();

            sendMessage("Welcome, " + username + ".");
            sendMessage("Use /help to see available commands.");
            
            String line;
            
            while (running && (line = in.readLine()) != null) {
                Command cmd = CommandParser.parse(line);
                
                switch (cmd.getType()) {
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
        sendMessage("Available commands:");
        sendMessage("/join <room> - join or create a room");
        sendMessage("/leave       - leave current room");
        sendMessage("/rooms       - list available rooms");
        sendMessage("/quit        - exit");
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
            return;
        }

        currentRoom = roomManager.getOrCreateRoom(roomName);
        currentRoom.addClient(this);

        sendMessage("Joined room: " + currentRoom.getName());
    }

    private void handleLeave() {
        if (currentRoom == null) {
            sendMessage("You are not in a room.");
            return;
        }

        String roomName = currentRoom.getName();
        currentRoom.removeClient(this);
        currentRoom = null;

        sendMessage("You left room: " + roomName);
    }

    private void handleMessage(String message) {
        if (currentRoom == null) {
            sendMessage("You must join a room before sending messages. Use: /join <room-name>");
            sendMessage("Available rooms: " + roomManager.listRooms());
            return;
        }

        System.out.println("From room: " + currentRoom.getName() + ", user: " + username + ", sent message: " + message);
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
}

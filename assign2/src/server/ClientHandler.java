package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import room.ChatRoom;
import room.RoomManager;

public class ClientHandler implements Runnable {
	private final Socket clientSocket;
    private final RoomManager roomManager;
    private ChatRoom currentRoom;
    private PrintWriter out;

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
            
            String line;
            
            while ((line = in.readLine()) != null) {
                if (line.startsWith("/join ")) {
                    String roomName = line.substring("/join ".length()).trim();
                    
                    if (currentRoom != null) {
                        currentRoom.removeClient(this);
                    }

                    currentRoom = roomManager.getOrCreateRoom(roomName);
                    currentRoom.addClient(this);
                    
                    sendMessage("Joined room: " + currentRoom.getName());
                    sendMessage("Available rooms: " + roomManager.listRooms());
                }
                else {
                    if (currentRoom == null) {
                        sendMessage("You must join a room before sending messages. Use: /join <room-name>");
                        sendMessage("Available rooms: " + roomManager.listRooms());
                        continue;
                    }

                    System.out.println("From room: " + currentRoom.getName() + ", received message: " + line);
                    currentRoom.broadcastExcept(line, this);
                }
            }
		} catch (Exception e) {
			System.err.println("Client handler error: " + e.getMessage());
		} finally {
            if (currentRoom != null) {
                currentRoom.removeClient(this);
            }
        }
	}
}

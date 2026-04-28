package server;

import java.net.ServerSocket;
import java.net.Socket;

import room.RoomManager;

public class ChatServer {
    public static void main(String[] args) throws Exception {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port);

            RoomManager roomManager = new RoomManager();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(new ClientHandler(clientSocket, roomManager));
            }
        }
    }
}

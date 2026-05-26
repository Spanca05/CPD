package server;

import java.net.ServerSocket;
import java.net.Socket;
import utils.SSLUtils;

import auth.AuthService;
import auth.TokenService;
import persistence.FileUserRepository;
import persistence.UserRepository;
import room.RoomManager;

public class ChatServer {
    public static void main(String[] args) throws Exception {
        int port = 12345;

        try (ServerSocket serverSocket = SSLUtils.createServerSocket(port)) {
            System.out.println("Secure server running on port " + port);

            UserRepository userRepository = new FileUserRepository("users.txt");
            AuthService authService = new AuthService(userRepository);
            RoomManager roomManager = new RoomManager();
            TokenService tokenService = new TokenService();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(new ClientHandler(clientSocket, roomManager, authService, tokenService));
            }
        }
    }
}

package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
	private final Socket clientSocket;
    private final ConnectionManager connectionManager;
    private PrintWriter out;

	public ClientHandler(Socket clientSocket, ConnectionManager connectionManager) {
		this.clientSocket = clientSocket;
        this.connectionManager = connectionManager;
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

            connectionManager.addClient(this);

            String line;

            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                connectionManager.broadcast(line, this);
            }
            connectionManager.removeClient(this);
		} catch (Exception e) {
			System.err.println("Client handler error: " + e.getMessage());
		}
	}
}

package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
	private final Socket clientSocket;

	public ClientHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try (
            Socket socket = clientSocket;
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true
            );
        ) {
            String line;

            while ((line = in.readLine()) != null) {
                System.out.println("Client: " + line);
                out.println("Echo: " + line);
            }
		} catch (Exception e) {
			System.err.println("Client handler error: " + e.getMessage());
		}
	}
}

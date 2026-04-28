package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
        )
        {
            System.out.println("Connected to server.");

            Thread.startVirtualThread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println();
                        System.out.println(line);
                        System.out.print("> ");
                    }
                } catch (Exception e) {
                    System.out.println("Disconnected from server.");
                }
            });

            while (true) {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("/quit")) {
                    out.println(message);
                    break;
                }

                out.println(message);
            }
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}

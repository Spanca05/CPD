package client;

import java.io.IOException;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        ClientConnection connection = new ClientConnection(host, port);

        try (Scanner scanner = new Scanner(System.in);) {
            connection.connect();
            System.out.println("Connected to server.");

            Thread.startVirtualThread(() -> {
                while (true) {
                    try {
                        String line;

                        while ((line = connection.readLine()) != null) {
                            System.out.println();
                            System.out.println(line);

                            if (line.startsWith("TOKEN ")) {
                                String token = line.substring("TOKEN ".length()).trim();
                                connection.setToken(token);
                            }

                            System.out.print("> ");
                        }

                        throw new IOException("Connection closed.");
                    } catch (IOException e) {
                        System.out.println();
                        System.out.println("Connection lost.");

                        if (!connection.hasToken()) {
                            System.out.println("No token available. Please login again.");
                            break;
                        }

                        try {
                            System.out.println("Reconnecting...");
                            connection.reconnect();
                            System.out.println("Reconnected.");
                            System.out.println("> ");
                        } catch (IOException reconnectError) {
                            System.out.println("Reconnect failed: " + reconnectError.getMessage());
                            break;
                        }
                    }
                }
            });

            while (true) {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("/quit")) {
                    connection.send(message);
                    break;
                }

                if (message.equalsIgnoreCase("/disconnect")) {
                    connection.close();
                    continue;
                }
                
                try {
                    connection.send(message);
                } catch (IOException e) {
                    System.out.println("Failed to send message: " + e.getMessage());
                    continue;
                }

            }
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            connection.close();
        }
    }
}

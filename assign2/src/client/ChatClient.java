package client;

import java.io.IOException;
import java.util.Scanner;

public class ChatClient {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String BLUE = "\u001B[34m";

    private static final Object CONSOLE_LOCK = new Object();

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        ClientConnection connection = new ClientConnection(host, port);

        try (Scanner scanner = new Scanner(System.in);) {
            connection.connect();
            printBanner();
            printSystem("Connected to server.", GREEN);

            if (connection.hasToken()) {
                printSystem("Saved token found. Attempting reconnect...", YELLOW);
                connection.send("/reconnect " + connection.getToken());
            }

            Thread.startVirtualThread(() -> {
                while (true) {
                    try {
                        String line;

                        while ((line = connection.readLine()) != null) {
                            if (line.startsWith("Invalid or expired token")) {
                                connection.clearToken();
                            }

                            if (line.startsWith("TOKEN ")) {
                                String token = line.substring("TOKEN ".length()).trim();
                                connection.setToken(token);
                                printSystem("Session token received.", DIM);
                                continue;
                            }

                            printIncoming(line);
                        }

                        throw new IOException("Connection closed.");
                    } catch (IOException e) {
                        printSystem("Connection lost.", RED);

                        if (!connection.hasToken()) break;

                        try {
                            printSystem("Reconnecting...", YELLOW);
                            connection.reconnect();
                            printSystem("Reconnected.", GREEN);
                        } catch (IOException reconnectError) {
                            printSystem("Reconnect failed: " + reconnectError.getMessage(), RED);
                            break;
                        }
                    }
                }
            });

            printSystem("Type /help for commands, /quit to exit.", DIM);

            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("/quit")) {
                    connection.clearToken();
                    printOutgoing("/quit");
                    connection.send(message);
                    break;
                }


                if (message.equalsIgnoreCase("/disconnect")) {
                    printSystem("Disconnected from server.", YELLOW);
                    connection.close();
                    continue;
                }
                
                try {
                    printOutgoing(message);
                    connection.send(message);
                } catch (IOException e) {
                    printSystem("Failed to send message: " + e.getMessage(), RED);
                    continue;
                }

            }
        } catch (Exception e) {
            printSystem("Client error: " + e.getMessage(), RED);
        } finally {
            connection.close();
        }
    }

    private static void printBanner() {
        synchronized (CONSOLE_LOCK) {
            System.out.println();
            System.out.println(CYAN + BOLD + "==============================" + RESET);
            System.out.println(CYAN + BOLD + "       Secure Chat Client     " + RESET);
            System.out.println(CYAN + BOLD + "==============================" + RESET);
            System.out.println();
        }
    }

    private static void printIncoming(String line) {
        synchronized (CONSOLE_LOCK) {
            System.out.println();
            int idx = line.indexOf(": ");
            if (idx > 0) {
                int offset = 0;
                String nameColor = MAGENTA;

                boolean userCheck = line.startsWith("//USER//");
                if (userCheck) {
                    nameColor = CYAN;
                    offset = 8;
                }

                String name = line.substring(offset, idx).trim();
                String msg = line.substring(idx + 2);


                System.out.println(nameColor + BOLD + "[" + name + "]" + RESET + " " + msg);
            } else {
                System.out.println(BLUE + BOLD + "[server]" + RESET + " " + line);
            }
        }
    }

    private static void printOutgoing(String message) {
        synchronized (CONSOLE_LOCK) {
            clearPreviousConsoleLine();
            System.out.println();
            System.out.println(GREEN + BOLD + "[you]" + RESET + " " + message + RESET);
        }
    }

    private static void clearPreviousConsoleLine() {
        synchronized (CONSOLE_LOCK) {
            final String ESC = "\u001B";
            System.out.print(ESC + "[1A" + ESC + "[2K");
            System.out.flush();
        }
    }

    private static void printSystem(String message, String color) {
        synchronized (CONSOLE_LOCK) {
            System.out.println();
            System.out.println(color + BOLD + "[info]" + RESET + " " + message);
        }
    }
}

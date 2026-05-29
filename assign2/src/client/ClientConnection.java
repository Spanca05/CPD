package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import utils.SSLUtils;

public class ClientConnection {
    private static final Path TOKEN_FILE = Path.of(System.getProperty("user.home"), ".cpd_chat_token");

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String token;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
        this.token = loadTokenFromDisk();
    }

    public void connect() throws IOException {
        try {
            socket = SSLUtils.createClientSocket(host, port);
        } catch (Exception e) {
            throw new IOException("Failed to create SSL socket: " + e.getMessage(), e);
        }

        in = new BufferedReader(
            new InputStreamReader(socket.getInputStream())
        );

        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String readLine() throws IOException {
        return in.readLine();
    }

    public void send(String message) throws IOException {
        if (out == null) {
            throw new IOException("Not connected.");
        }

        out.println(message);

        if (out.checkError()) {
            throw new IOException("Failed to send message.");
        }
    }

    public void reconnect() throws IOException {
        if (token == null || token.isBlank()) {
            throw new IOException("No token available for reconnect.");
        }

        close();
        connect();
        send("/reconnect " + token);
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {       
        } finally {
            socket = null;
            in = null;
            out = null;
        }
    }

    public void setToken(String token) {
        this.token = token;
        persistToken(token);
    }

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    public String getToken() {
        return token;
    }

    public void clearToken() {
        token = null;

        try {
            Files.deleteIfExists(TOKEN_FILE);
        } catch (IOException ignored) {
        }
    }

    private String loadTokenFromDisk() {
        try {
            if (Files.exists(TOKEN_FILE)) {
                String stored = Files.readString(TOKEN_FILE, StandardCharsets.UTF_8).trim();
                if (!stored.isBlank()) {
                    return stored;
                }
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    private void persistToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                Files.deleteIfExists(TOKEN_FILE);
                return;
            }

            Files.writeString(TOKEN_FILE, token.trim(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}

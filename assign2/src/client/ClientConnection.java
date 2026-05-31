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
    private static final Path TOKEN_DIR = Path.of(System.getProperty("user.home"), ".cpd_chat_tokens");

    private final String host;
    private final int port;
    private final String clientId;
    private final Path tokenFile;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String token;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
        this.clientId = sanitizeForFileName(System.getProperty("cpd.clientId", String.valueOf(System.nanoTime())));
        this.tokenFile = buildTokenFilePath(host, port, clientId);
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
            Files.deleteIfExists(tokenFile);
        } catch (IOException ignored) {
        }
    }

    private String loadTokenFromDisk() {
        try {
            if (Files.exists(tokenFile)) {
                String stored = Files.readString(tokenFile, StandardCharsets.UTF_8).trim();
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
                Files.deleteIfExists(tokenFile);
                return;
            }

            Files.createDirectories(tokenFile.getParent());
            Files.writeString(tokenFile, token.trim(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private Path buildTokenFilePath(String host, int port, String clientId) {
        String endpointId = sanitizeForFileName(host + "_" + port);
        return TOKEN_DIR.resolve("cpd_chat_token_" + clientId + "_" + endpointId + ".txt");
    }

    private String sanitizeForFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

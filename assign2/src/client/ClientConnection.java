package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import utils.SSLUtils;

public class ClientConnection {
    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String token;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
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
    }

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }
}

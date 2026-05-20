package room;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import server.ClientHandler;
import utils.ThreadSafeList;
import utils.ThreadSafeQueue;

public class ChatRoom {
    private final String name;
    private final ThreadSafeList<ClientHandler> clients = new ThreadSafeList<>();

    private volatile boolean aiRoom = false;
    private volatile String prompt = null;

    private final ThreadSafeQueue<String> messages = new ThreadSafeQueue<>();
    private final int maxHistory = 200;

    private final AtomicBoolean aiRequestInFlight = new AtomicBoolean(false);
    private final ThreadSafeQueue<String> pendingAiRequests = new ThreadSafeQueue<>();

    public ChatRoom(String name) {
        this.name = name;
    }

    public boolean isAiRoom() {
        return aiRoom;
    }

    public void setAiRoom(boolean ai, String prompt) {
        this.aiRoom = ai;
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
        sendHistoryTo(client);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    private void addMessageToHistory(String message) {
        messages.addLast(message);
        while (messages.size() > maxHistory) {
            messages.removeFirst();
        }
    }

    public void addMessage(String message) {
        addMessageToHistory(message);
    }

    private List<String> getHistorySnapshot() {
        return messages.snapshot();
    }

    public List<String> getHistory() {
        return getHistorySnapshot();
    }

    public void sendHistoryTo(ClientHandler client) {
        List<String> snapshot = getHistorySnapshot();
        for (String m : snapshot) {
            client.sendMessage(m);
        }
    }

    public void broadcastExcept(String message, ClientHandler sender) {
        addMessageToHistory(message);

        List<ClientHandler> recipients = clients.snapshot();

        for (ClientHandler client : recipients) {
            if (client != sender) {
                Thread.startVirtualThread(() -> client.sendMessage(message));
            }
        }
    }

    public void broadcast(String message) {
        addMessageToHistory(message);

        List<ClientHandler> recipients = clients.snapshot();

        for (ClientHandler client : recipients) {
            Thread.startVirtualThread(() -> client.sendMessage(message));
        }
    }

    public boolean startAiRequestIfIdle(String context) {
        boolean wasInFlight = aiRequestInFlight.getAndSet(true);
        if (wasInFlight) {
            pendingAiRequests.addLast(context);
            return false;
        } else {
            return true;
        }
    }

    public String finishAiRequestAndGetPending() {
        if (!pendingAiRequests.isEmpty()) {
            return pendingAiRequests.removeFirst();
        } else {
            aiRequestInFlight.set(false);
            return null;
        }
    }

    public String getName() {
        return this.name;
    }
}

package room;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import server.ClientHandler;

public class ChatRoom {
    private String name;
    private List<ClientHandler> clients = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private boolean aiRoom = false;
    private String prompt = null;

    private final Deque<String> messages = new ArrayDeque<>();
    private final int maxHistory = 200;

    private boolean aiRequestInFlight = false;
    private final Deque<String> pendingAiRequests = new ArrayDeque<>();
    private final ReentrantLock aiLock = new ReentrantLock();

    public ChatRoom(String name) {
        this.name = name;
    }

    public boolean isAiRoom() {
        lock.lock();
        try {
            return aiRoom;
        } finally {
            lock.unlock();
        }
    }

    public void setAiRoom(boolean ai, String prompt) {
        lock.lock();
        try {
            this.aiRoom = ai;
            this.prompt = prompt;
        } finally {
            lock.unlock();
        }
    }

    public String getPrompt() {
        lock.lock();
        try {
            return prompt;
        } finally {
            lock.unlock();
        }
    }

    public void addClient(ClientHandler client) {
        lock.lock();
        try {
            clients.add(client);
        } finally {
            lock.unlock();
        }

        sendHistoryTo(client);
    }

    public void removeClient(ClientHandler client) {
        lock.lock();
        try {
            clients.remove(client);
        } finally {
            lock.unlock();
        }
    }

    private void addMessageToHistory(String message) {
        lock.lock();
        try {
            messages.addLast(message);
            while (messages.size() > maxHistory) {
                messages.removeFirst();
            }
        } finally {
            lock.unlock();
        }
    }

    public void addMessage(String message) {
        addMessageToHistory(message);
    }

    private List<String> getHistorySnapshot() {
        lock.lock();
        try {
            return new ArrayList<>(messages);
        } finally {
            lock.unlock();
        }
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

        List<ClientHandler> recipients = new ArrayList<>();

        lock.lock();
        try {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    recipients.add(client);
                }
            }
        } finally {
            lock.unlock();
        }

        for (ClientHandler client : recipients) {
            Thread.startVirtualThread(() -> client.sendMessage(message));
        }
    }

    public void broadcast(String message) {
        addMessageToHistory(message);

        List<ClientHandler> recipients = new ArrayList<>();

        lock.lock();
        try {
            recipients.addAll(clients);
        } finally {
            lock.unlock();
        }

        for (ClientHandler client : recipients) {
            Thread.startVirtualThread(() -> client.sendMessage(message));
        }
    }

    public boolean startAiRequestIfIdle(String context) {
        aiLock.lock();
        try {
            if (aiRequestInFlight) {
                pendingAiRequests.addLast(context); 
                return false;
            } else {
                aiRequestInFlight = true;
                return true;
            }
        } finally {
            aiLock.unlock();
        }
    }

    public String finishAiRequestAndGetPending() {
        aiLock.lock();
        try {
            if (!pendingAiRequests.isEmpty()) {
                String next = pendingAiRequests.removeFirst(); 
                return next;
            } else {
                aiRequestInFlight = false;
                return null;
            }
        } finally {
            aiLock.unlock();
        }
    }

    public String getName() {
        return this.name;
    }
}

package room;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import server.ClientHandler;

public class ChatRoom {
    private String name;
    private List<ClientHandler> clients = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public ChatRoom(String name) {
        this.name = name;
    }


    public void addClient(ClientHandler client) {
        lock.lock();
        try {
            clients.add(client);
        } finally {
            lock.unlock();
        }
    }

    public void removeClient(ClientHandler client) {
        lock.lock();
        try {
            clients.remove(client);
        } finally {
            lock.unlock();
        }
    }

    public void broadcastExcept(String message, ClientHandler sender) {
        lock.lock();
        try {
            for (ClientHandler client : clients) {
                if (client == sender)
                    continue;
                client.sendMessage(message);
            }
        } finally {
            lock.unlock();
        }
    }

    public String getName() {
        return this.name;
    }
}

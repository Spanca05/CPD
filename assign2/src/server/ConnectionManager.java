package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import client.ChatClient;

public class ConnectionManager {
    private final List<ClientHandler> clients = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public void addClient(ClientHandler client) {
        lock.lock();
        try {
            clients.add(client);
        } 
        finally {
            lock.unlock();
        } 
    }

    public void removeClient(ClientHandler client) {
        lock.lock();
        try {
            clients.remove(client);
        }
        finally {
            lock.unlock();
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        lock.lock();
        try {
            for (ClientHandler client : clients) {
                if (client == sender) continue;
                client.sendMessage(message);
            }
        }
        finally {
            lock.unlock();
        }
    }
}

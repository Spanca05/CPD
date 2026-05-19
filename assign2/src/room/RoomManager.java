package room;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class RoomManager {
    private final Map<String ,ChatRoom> rooms = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public RoomManager() {
        rooms.put("default", new ChatRoom("default"));
    }

    public ChatRoom getOrCreateRoom(String name) {
        lock.lock();
        try {
            ChatRoom room = rooms.get(name);

            if (room == null) {
                room = new ChatRoom(name);
                rooms.put(name, room);
            }

            return room;
        } finally {
            lock.unlock();
        }
    }

    public String listRooms() {
        lock.lock();
        try {
            return String.join(", ", rooms.keySet());
        } finally {
            lock.unlock();
        }
    }

    public void addRoom(String name, ChatRoom room) {
        lock.lock();
        try {
            rooms.put(name, room);
        } finally {
            lock.unlock();
        }
    }

    public void removeRoom(String name) {
        lock.lock();
        try {
            rooms.remove(name);
        } finally {
            lock.unlock();
        }
    }

    public ChatRoom createAiRoom(String name, String prompt) {
        ChatRoom room = getOrCreateRoom(name);
        room.setAiRoom(true, prompt);
        return room;
    }

}

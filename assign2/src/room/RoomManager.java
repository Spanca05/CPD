package room;

import utils.ThreadSafeMap;

public class RoomManager {
    private final ThreadSafeMap<String, ChatRoom> rooms = new ThreadSafeMap<>();

    public RoomManager() {
        rooms.put("default", new ChatRoom("default"));
    }

    public ChatRoom getOrCreateRoom(String name) {
        return rooms.getOrCreate(name, () -> new ChatRoom(name));
    }

    public String listRooms() {
        return String.join(", ", rooms.keySnapshot());
    }

    public void addRoom(String name, ChatRoom room) {
        rooms.put(name, room);
    }

    public void removeRoom(String name) {
        rooms.remove(name);
    }

    public ChatRoom createAiRoom(String name, String prompt) {
        ChatRoom room = getOrCreateRoom(name);
        room.setAiRoom(true, prompt);
        return room;
    }

}

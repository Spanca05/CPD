package model;

public class TokenInfo {
    private final String username;
    private String currentRoomName;
    private final long expiresAt;

    public TokenInfo(String username, String currentRoomName, long expiresAt) {
        this.username = username;
        this.currentRoomName = currentRoomName;
        this.expiresAt = expiresAt;
    }

    public String getUsername() {
        return this.username;
    }

    public String getCurrentRoomName() {
        return this.currentRoomName;
    }

    public void setCurrentRoomName(String currentRoomName) {
        this.currentRoomName = currentRoomName;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}

package auth;

import java.util.UUID;

import model.TokenInfo;
import utils.ThreadSafeMap;

public class TokenService {
    private final ThreadSafeMap<String, TokenInfo> tokens = new ThreadSafeMap<>();

    public String createToken(String username) {
        String token = UUID.randomUUID().toString();

        long expiresAt = System.currentTimeMillis() + 60 * 60 * 1000;

        tokens.put(token, new TokenInfo(username, null, expiresAt));

        return token;
    }

    public TokenInfo validateToken(String token) {
        TokenInfo info = tokens.get(token);

        if (info == null || info.isExpired()) {
            tokens.remove(token);
            return null;
        }

        return info;
    }

    public void updateRoom(String token, String roomName) {
        TokenInfo info = tokens.get(token);
        if (info != null && !info.isExpired()) {
            info.setCurrentRoomName(roomName);
        }
    }

    public void removeToken(String token) {
        tokens.remove(token);
    }
}
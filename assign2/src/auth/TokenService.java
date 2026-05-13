package auth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.TokenInfo;

public class TokenService {
    private final Map<String, TokenInfo> tokens = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    public String createToken(String username) {
        String token = UUID.randomUUID().toString();

        long expiresAt = System.currentTimeMillis() + 60 * 60 * 1000;

        lock.lock();
        try {
            tokens.put(token, new TokenInfo(username, null, expiresAt));
        } finally {
            lock.unlock();
        }

        return token;
    }

    public TokenInfo validateToken(String token) {
        lock.lock();
        try {
            TokenInfo info = tokens.get(token);

            if (info == null || info.isExpired()) {
                tokens.remove(token);
                return null;
            }

            return info;
        } finally {
            lock.unlock();
        }
    }

    public void updateRoom(String token, String roomName) {
        lock.lock();
        try {
            TokenInfo info = tokens.get(token);
            if (info != null && !info.isExpired()) {
                info.setCurrentRoomName(roomName);
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeToken(String token) {
        lock.lock();
        try {
            tokens.remove(token);
        } finally {
            lock.unlock();
        }
    }
}
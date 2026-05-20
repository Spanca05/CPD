package persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import auth.PasswordService;
import utils.ThreadSafeMap;

public class FileUserRepository implements UserRepository {
    private final Path filePath;
    private final ThreadSafeMap<String, String> users = new ThreadSafeMap<>();
    private final Lock fileLock = new ReentrantLock();

    public FileUserRepository(String filename) {
        this.filePath = Paths.get(filename);

        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }

            List<String> content = Files.readAllLines(filePath);

            for (String line : content) {
                String[] tokens = line.split(":", 2);

                if (tokens.length < 2) {
                    continue;
                }

                users.put(tokens[0].trim(), tokens[1].trim());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating user file", e);
        }
    }

    @Override
    public boolean userExists(String username) {
        return users.containsKey(username.trim());
    }

    @Override
    public void saveUser(String username, String password) {
        final String normalizedUsername = username.trim();

        fileLock.lock();
        try {
            if (users.containsKey(normalizedUsername)) {
                throw new RuntimeException("User already exists.");
            }
            
            String passwordValue = PasswordService.hashPassword(password);

            try {
                Files.writeString(
                        filePath,
                        normalizedUsername + ":" + passwordValue + System.lineSeparator(),
                        StandardOpenOption.APPEND);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write user to file", e);
            }

            users.put(normalizedUsername, passwordValue);
        } finally {
            fileLock.unlock();
        }
    }

    @Override
    public boolean validateUser(String username, String password) {
        username = username.trim();

        String storedHash = users.get(username);

        if (storedHash == null) {
            return false;
        }

        return PasswordService.verifyPassword(password, storedHash);
    }
}

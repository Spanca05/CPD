package persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileUserRepository implements UserRepository {
    private final Path filePath;
    private final Lock lock = new ReentrantLock();

    public FileUserRepository(String filename) {
        this.filePath = Paths.get(filename);

        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating user file", e);
        }
    }

    @Override
    public boolean userExists(String username) {
        username = username.trim();

        lock.lock();

        try {
            List<String> content = Files.readAllLines(filePath);

            for (String line : content) {
                String[] tokens = line.split(":");

                if (tokens.length < 2)
                    continue;

                String name = tokens[0];

                if (username.equals(name.trim())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't read from file.", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void saveUser(String username, String password) {
        username = username.trim();
        password = password.trim();

        lock.lock();

        try {

            if (userExists(username)) {
                throw new RuntimeException("User already exists.");
            }

            Files.writeString(
                    filePath,
                    username.trim() + ":" + password.trim() + System.lineSeparator(),
                    StandardOpenOption.APPEND);

        } catch (Exception e) {
            throw new RuntimeException("Failed to write user to file", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean validateUser(String username, String password) {
        username = username.trim();
        password = password.trim();

        lock.lock();

        try {
            List<String> content = Files.readAllLines(filePath);

            for (String line : content) {
                String[] tokens = line.split(":", 2);

                if (tokens.length < 2) continue;

                String storedUsername = tokens[0].trim();
                String storedPassword = tokens[1].trim();

                if (username.equals(storedUsername) && password.equals(storedPassword)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate user", e);
        } finally {
            lock.unlock();
        }
    }
}

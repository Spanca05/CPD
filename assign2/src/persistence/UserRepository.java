package persistence;

public interface UserRepository {
    boolean userExists(String username);
    void saveUser(String username, String password);
    boolean validateUser(String username, String password);
}

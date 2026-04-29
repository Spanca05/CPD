package auth;

import persistence.UserRepository;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean register(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            return false;
        }

        if (userRepository.userExists(username)) {
            return false;
        }

        userRepository.saveUser(username, password);
        return true;
    }

    public boolean login(String username, String password) {
        return userRepository.validateUser(username, password);
    }
}

package com.kidsgame.mathapp.config;

import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class AdminAccountSeeder implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminAccountSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap.username:}") String username,
            @Value("${app.admin.bootstrap.password:}") String password,
            @Value("${app.admin.bootstrap.display-name:Администратор}") String displayName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.displayName = displayName == null || displayName.isBlank() ? "Администратор" : displayName.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD must be at least 12 characters.");
        }

        UserEntity admin = userRepository.findByUsernameIgnoreCase(username)
                .map(existing -> {
                    existing.updateIdentity(existing.getUsername(), displayName);
                    existing.updateRole(Role.ADMIN);
                    if (!passwordEncoder.matches(password, existing.getPasswordHash())) {
                        existing.updatePassword(passwordEncoder.encode(password));
                    }
                    return existing;
                })
                .orElseGet(() -> new UserEntity(username, displayName, passwordEncoder.encode(password), Role.ADMIN));
        userRepository.save(admin);
    }
}

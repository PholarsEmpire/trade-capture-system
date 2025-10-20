package com.technicalchallenge.runner;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.repository.ApplicationUserRepository;

// This runner or utility class that migrates existing user passwords (which are currently in plain text) to BCrypt encoding on application startup.
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private final ApplicationUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(ApplicationUserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        List<ApplicationUser> users = userRepository.findAll();
        for (ApplicationUser u : users) {
            String raw = u.getPassword();
            if (raw == null || raw.isBlank()) continue;

            // Heuristic: determine if password already looks like BCrypt (starts with $2a$ or $2b$)
            if (raw.startsWith("$2a$") || raw.startsWith("$2b$") || raw.startsWith("{bcrypt}")) {
                // Already bcrypt-encoded — skip or normalize
                if (raw.startsWith("{bcrypt}")) {
                    u.setPassword(raw.substring("{bcrypt}".length())); // optional cleanup
                    userRepository.save(u);
                }
                continue;
            }

            // If it's plain text, encode and save. You could also prefix with {bcrypt}
            String encoded = passwordEncoder.encode(raw);
            u.setPassword(encoded);
            userRepository.save(u);

            System.out.println("Migrated password for user: " + u.getLoginId());
        }

        // Once the application runs and passwords are migrated, I will disable or remove this runner to avoid re-encoding.
    }
}

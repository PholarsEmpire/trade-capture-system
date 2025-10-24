package com.technicalchallenge.security;

import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordConfig {

    // Password encoder bean for encoding passwords
    // This is a global encoder used throughout the application. This ensures consistency in password handling and only has to be injected where needed.
    // BCrypt is a strong hashing function suitable for password storage and verification
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

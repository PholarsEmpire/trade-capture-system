// package com.technicalchallenge.security;

// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// // This class is automatically detected and run by Spring Boot
// @Configuration
// public class StartupHashedPasswordGenerator {
//     private static final Logger logger = LoggerFactory.getLogger(StartupHashedPasswordGenerator.class);

//     // Spring injects the PasswordEncoder bean you defined in SecurityConfig
//     private final PasswordEncoder passwordEncoder;

//     public StartupHashedPasswordGenerator(PasswordEncoder passwordEncoder) {
//         this.passwordEncoder = passwordEncoder;
//     }

//     /**
//      * Defines a bean that runs code immediately after the Spring context is loaded.
//      */
//     @Bean
//     public CommandLineRunner generatePasswordHash() {
//         return args -> {
//             String plainPassword = "password";
            
//             // Encodes the plain password using the configured DelegatingPasswordEncoder (which uses BCrypt)
//             String encodedPassword = passwordEncoder.encode(plainPassword);
            
//             // The DelegatingPasswordEncoder requires the {bcrypt} prefix in the DB.
//             String dataSqlValue = "{bcrypt}" + encodedPassword;

//             logger.info("==================================================================");
//             logger.info("🔑 GENERATED PASSWORD HASH FOR: {}", plainPassword);
//             logger.info("   -> Copy THIS Value to your data.sql file:");
//             logger.info("   -> {}", dataSqlValue);
//             logger.info("==================================================================");
//         };
//     }
// }
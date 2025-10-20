package com.technicalchallenge.security;

import com.technicalchallenge.repository.ApplicationUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

// Custom UserDetailsService to load user-specific data
// This is used by Spring Security during authentication
// It fetches user details from the database and converts them
// into a UserDetails object that Spring Security can work with
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private ApplicationUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        var user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginId));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is inactive: " + loginId);
        }

        // Convert to Spring Security UserDetails 
        // Note: Password should be encoded in the database for advanced security
        // I have a file called PasswordConfig.java that sets up password encoding
        // and I use BCryptPasswordEncoder to encode passwords in the ApplicationUserSevice before saving them new users.
        // The below converts ApplicationUser to a UserDetails object that Spring understands.
        return User.builder()
                .username(user.getLoginId())
                .password(user.getPassword())  
                .roles(user.getUserProfile().getUserType())  // e.g. ADMIN, SUPERUSER
                .build();
    }
}

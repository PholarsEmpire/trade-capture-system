// package com.technicalchallenge.security;

// import java.util.List;

// //import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.*;
// import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// //import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// //import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// import org.springframework.security.config.annotation.web.builders.*;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// //import org.springframework.security.config.http.SessionCreationPolicy;
// //import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// import org.springframework.web.filter.CorsFilter;


// // Security configuration class to set up authentication and authorization
// // It defines which endpoints are secured and which are publicly accessible
// // It also configures the authentication provider to use our custom UserDetailsService
// // and password encoder for verifying user credentials
// // This class is essential for integrating Spring Security into the application
// // and ensuring that only authenticated users can access protected resources

// @Configuration
// @EnableWebSecurity
// @EnableMethodSecurity(prePostEnabled = true)
// public class SecurityConfig {

//     @Bean
//     public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
//         MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

//         http
//             .csrf(csrf -> csrf.disable()) // ✅ disable CSRF for API calls
//             .cors(cors -> cors.disable()) // optional if React handles CORS via proxy
//             .authorizeHttpRequests(auth -> auth
//                 // ✅ Allow Swagger and API documentation endpoints
//                 .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
//                 .requestMatchers(mvc.pattern("/v3/api-docs/**")).permitAll()
//                 .requestMatchers(mvc.pattern("/api-docs/**")).permitAll()
//                 .requestMatchers(mvc.pattern("/swagger-resources/**")).permitAll()

//                 // ✅ Allow H2 console
//                 .requestMatchers(mvc.pattern("/h2-console/**")).permitAll()

//                 // ✅ Allow all frontend and API routes (adjust if needed)
//                 .requestMatchers(mvc.pattern("/api/**")).permitAll()
//                 .requestMatchers(mvc.pattern("/trades/**")).permitAll()

//                 // ✅ Everything else still secured if you want to restrict admin endpoints
//                 .anyRequest().permitAll()
//             )

//             // ✅ Remove the default login form and HTTP Basic popup
//             .formLogin(form -> form.disable())
//             .httpBasic(basic -> basic.disable())

//             // ✅ Allow Swagger + H2 console frames
//             .headers(headers -> headers.frameOptions(frame -> frame.disable()));

//         return http.build();
//     }



//     // CORS configuration  for allowing requests from the React frontend 
//      @Bean
//     public CorsFilter corsFilter() {
//         CorsConfiguration config = new CorsConfiguration();
//         config.setAllowCredentials(true);
//         config.setAllowedOrigins(List.of("http://localhost:5173")); // React frontend
//         config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//         config.setAllowedHeaders(List.of("*"));

//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", config);
//         return new CorsFilter(source);
//     }
// }





// package com.technicalchallenge.security;

// import java.util.List;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// import org.springframework.web.filter.CorsFilter;
// import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

// @Configuration
// @EnableWebSecurity
// @EnableMethodSecurity(prePostEnabled = true)
// public class SecurityConfig {

//     /**
//      * Defines how incoming HTTP requests are secured.
//      * - Public: Swagger, H2 console, authentication endpoints
//      * - Protected: All /api/** routes
//      */
//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
//         MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

//         http
//             // Disable CSRF for APIs (stateless REST calls)
//             .csrf(csrf -> csrf.disable())

//             // Enable CORS (configured below)
//             .cors(cors -> cors.configurationSource(corsConfigurationSource()))

//             // Authorization rules
//             .authorizeHttpRequests(auth -> auth
//                 // Publicly accessible endpoints (Swagger, H2, auth)
//                 .requestMatchers(
//                     mvc.pattern("/swagger-ui/**"),
//                     mvc.pattern("/v3/api-docs/**"),
//                     mvc.pattern("/swagger-resources/**"),
//                     mvc.pattern("/api-docs/**"),
//                     mvc.pattern("/h2-console/**"),
//                     mvc.pattern("/auth/**") // login/signup endpoints if you have them
//                 ).permitAll()

//                 // Protect all API endpoints
//                 .requestMatchers(mvc.pattern("/api/**")).authenticated()

//                 // Everything else (static files, root paths) allowed
//                 .anyRequest().permitAll()
//             )

//             // Basic HTTP authentication for testing (can swap for JWT later)
//             .httpBasic(basic -> {})

//             // Disable default login page (we’ll use frontend)
//             .formLogin(form -> form.disable())

//             // Allow embedded H2 console to render correctly
//             .headers(headers -> headers.frameOptions(frame -> frame.disable()));

//         return http.build();
//     }


//     /**
//      * CORS configuration to allow calls from the frontend (React).
//      */
//     @Bean
//     public UrlBasedCorsConfigurationSource corsConfigurationSource() {
//         CorsConfiguration config = new CorsConfiguration();
//         config.setAllowCredentials(true);
//         config.setAllowedOrigins(List.of("http://localhost:5173")); // Frontend origin
//         config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//         config.setAllowedHeaders(List.of("*"));

//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", config);
//         return source;
//     }

//     @Bean
//     public CorsFilter corsFilter() {
//         return new CorsFilter(corsConfigurationSource());
//     }
// }



package com.technicalchallenge.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordConfig passwordConfig;

    /**
     * Defines how requests are secured.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            

            .authorizeHttpRequests(auth -> auth

           
                // Publicly accessible endpoints
                .requestMatchers(
                    mvc.pattern("/swagger-ui/**"),
                    mvc.pattern("/v3/api-docs/**"),
                    mvc.pattern("/swagger-resources/**"),
                    mvc.pattern("/api-docs/**"),
                    mvc.pattern("/h2-console/**"),
                    mvc.pattern("/auth/**"),
                    mvc.pattern("/api/login/**")
                ).permitAll()

                 // Protected endpoints
               // .requestMatchers(mvc.pattern("/api/**")).authenticated()

                // Everything else public
                .anyRequest().permitAll()
            )
            // Use HTTP Basic for now (can switch to JWT later)
            .httpBasic(basic -> {})

            // Disable form login since frontend handles auth
            .formLogin(form -> form.disable())

            // Allow H2 console to render in browser
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }


    /**
     * Link the CustomUserDetailsService and password encoder.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordConfig.passwordEncoder());
        return provider;
    }

    /**
     * Expose AuthenticationManager for use in controllers (e.g. login endpoints).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Allow requests from React frontend during development.
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}

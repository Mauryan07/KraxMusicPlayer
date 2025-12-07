package com.exproject.simplemusicplayer.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // Ensure CustomUserDetailsService is a @Service bean in the context
    private final CustomUserDetailsService userDetailsService;

    /**
     * Configure SecurityFilterChain using non-deprecated, lambda-style APIs.
     * We do not manually instantiate DaoAuthenticationProvider here — Spring will auto-configure
     * a DaoAuthenticationProvider that uses the UserDetailsService and PasswordEncoder beans.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(authorize -> authorize
                        // public routes (browsing & auth)
                        .requestMatchers(
                                "/",
                                "/home/**",
                                "/api/home/**",
                                "/api/auth/**",
                                "/api/albums",
                                "/api/album/*",
                                "/api/listTracks",
                                "/api/tracks",
                                "/api/search/**",
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // explicitly allow GET streaming endpoints (single-segment fileHash)
                        .requestMatchers(HttpMethod.GET, "/api/track/*/audio", "/api/track/*/artwork").permitAll()
                        // everything else requires authentication
                        .anyRequest().authenticated()
                )

                .httpBasic(Customizer.withDefaults())

                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    /**
     * Expose AuthenticationManager if programmatic access is required.
     * Spring Boot will wire AuthenticationManager using available UserDetailsService + PasswordEncoder.
     */
    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
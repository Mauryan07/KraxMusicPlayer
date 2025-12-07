package com.exproject.simplemusicplayer.config;

import com.exproject.simplemusicplayer.model.Role;
import com.exproject.simplemusicplayer.model.User;
import com.exproject.simplemusicplayer.repository.RoleRepository;
import com.exproject.simplemusicplayer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner init() {
        return args -> {
            Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

            // Create initial admin if no users exist
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin")) // change when deployed
                        .enabled(true)
                        .roles(Set.of(userRole, adminRole))
                        .build();
                userRepository.save(admin);
                System.out.println("Initial admin created: username=admin password=adminpass");
            }
        };
    }
}
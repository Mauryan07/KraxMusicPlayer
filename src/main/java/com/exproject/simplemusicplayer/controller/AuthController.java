package com.exproject.simplemusicplayer.controller;

import com.exproject.simplemusicplayer.model.Role;
import com.exproject.simplemusicplayer.model.User;
import com.exproject.simplemusicplayer.repository.RoleRepository;
import com.exproject.simplemusicplayer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            return ResponseEntity.badRequest().body("Username already taken");
        }
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        User u = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(u);
        return ResponseEntity.ok("User registered");
    }

    /**
     * Returns info about the currently authenticated user.
     * Client should call this after a successful Basic auth request (or use browser's Basic auth prompt).
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfo> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new UserInfo(username, roles));
    }

    // DTOs
    public record RegisterRequest(String username, String password) {
    }

    public record UserInfo(String username, java.util.List<String> roles) {
    }
}
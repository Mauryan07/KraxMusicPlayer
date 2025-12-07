package com.exproject.simplemusicplayer.controller;

import com.exproject.simplemusicplayer.model.Role;
import com.exproject.simplemusicplayer.model.User;
import com.exproject.simplemusicplayer.repository.RoleRepository;
import com.exproject.simplemusicplayer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // Only admin can promote another user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/promote/{userId}")
    public String promoteToAdmin(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        user.getRoles().add(adminRole);
        userRepository.save(user);
        return "User promoted to admin";
    }
}
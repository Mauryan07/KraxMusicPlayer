package com.exproject.kraxmusicplayer.controller;

import com.exproject.kraxmusicplayer.dto.AdminMetricsDTO;
import com.exproject.kraxmusicplayer.dto.AdminUserDTO;
import com.exproject.kraxmusicplayer.model.Role;
import com.exproject.kraxmusicplayer.model.User;
import com.exproject.kraxmusicplayer.repository.RoleRepository;
import com.exproject.kraxmusicplayer.repository.UserRepository;
import com.exproject.kraxmusicplayer.service.AdminMetricsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminMetricsService adminMetricsService;

    public record AdminActionResponse(int statusCode, String message) {}

    private Role getRoleOrThrow(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found in DB: " + roleName));
    }

    private User getUserByUsernameOrThrow(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private static boolean isAdmin(User u) {
        return u.getRoles() != null && u.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()));
    }

    /**
     * REQUIRED by PromoteDialog
     * GET /api/admin/users
     * Returns: [{id, username, role}] where role is "admin" | "user"
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<AdminUserDTO> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(u -> new AdminUserDTO(
                        u.getId(),
                        u.getUsername(),
                        isAdmin(u) ? "admin" : "user"
                ))
                .toList();
    }

    /**
     * REQUIRED by PromoteDialog
     * POST /api/admin/toggleRole/{id}
     * Toggles ROLE_ADMIN for the given user id.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/toggleRole/{id}")
    @Transactional
    public AdminUserDTO toggleRole(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: id=" + id));
        Role adminRole = getRoleOrThrow("ROLE_ADMIN");

        boolean currentlyAdmin = isAdmin(user);
        if (currentlyAdmin) {
            user.getRoles().removeIf(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()));
        } else {
            user.getRoles().add(adminRole);
        }
        userRepository.save(user);

        return new AdminUserDTO(user.getId(), user.getUsername(), currentlyAdmin ? "user" : "admin");
    }

    /**
     * Promote by USERNAME (frontend can call this too if needed).
     * POST /api/admin/promote/{username}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/promote/{username}")
    @Transactional
    public ResponseEntity<AdminActionResponse> promoteToAdmin(@PathVariable String username) {
        User user = getUserByUsernameOrThrow(username);
        Role adminRole = getRoleOrThrow("ROLE_ADMIN");

        if (user.getRoles().contains(adminRole)) {
            return ResponseEntity.ok(new AdminActionResponse(
                    HttpStatus.OK.value(),
                    "User is already an admin: " + username
            ));
        }

        user.getRoles().add(adminRole);
        userRepository.save(user);

        return ResponseEntity.ok(new AdminActionResponse(
                HttpStatus.OK.value(),
                "User promoted to admin: " + username
        ));
    }

    /**
     * Demote by USERNAME.
     * POST /api/admin/demote/{username}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/demote/{username}")
    @Transactional
    public ResponseEntity<AdminActionResponse> demoteFromAdmin(@PathVariable String username) {
        User user = getUserByUsernameOrThrow(username);
        Role adminRole = getRoleOrThrow("ROLE_ADMIN");

        boolean removed = user.getRoles().remove(adminRole);
        userRepository.save(user);

        if (!removed) {
            return ResponseEntity.ok(new AdminActionResponse(
                    HttpStatus.OK.value(),
                    "User was not an admin: " + username
            ));
        }

        return ResponseEntity.ok(new AdminActionResponse(
                HttpStatus.OK.value(),
                "User demoted from admin: " + username
        ));
    }

    /**
     * Optional: promote by ID (kept for compatibility).
     * POST /api/admin/promote/id/{userId}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/promote/id/{userId}")
    @Transactional
    public ResponseEntity<AdminActionResponse> promoteToAdminById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found id=" + userId));
        Role adminRole = getRoleOrThrow("ROLE_ADMIN");

        if (!user.getRoles().contains(adminRole)) {
            user.getRoles().add(adminRole);
            userRepository.save(user);
        }

        return ResponseEntity.ok(new AdminActionResponse(
                HttpStatus.OK.value(),
                "User promoted to admin (id=" + userId + ")"
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/metrics")
    public AdminMetricsDTO metrics() {
        return adminMetricsService.getMetrics();
    }
}
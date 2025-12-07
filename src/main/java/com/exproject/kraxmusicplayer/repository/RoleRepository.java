package com.exproject.kraxmusicplayer.repository;

import com.exproject.kraxmusicplayer.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
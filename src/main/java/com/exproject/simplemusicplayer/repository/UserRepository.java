package com.exproject.simplemusicplayer.repository;

import com.exproject.simplemusicplayer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

package com.exproject.simplemusicplayer.repository;

import com.exproject.simplemusicplayer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

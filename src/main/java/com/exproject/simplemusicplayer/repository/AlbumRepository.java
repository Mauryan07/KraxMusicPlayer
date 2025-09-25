package com.exproject.simplemusicplayer.repository;

import com.exproject.simplemusicplayer.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByNameContainingIgnoreCase(String albumName);

    Optional<Album> findByNameIgnoreCase(String albumName);
}

package com.exproject.simplemusicplayer.repository;

import com.exproject.simplemusicplayer.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    List<Artist> findByNameContainingIgnoreCase(String artistName);
    Optional<Artist> findByNameIgnoreCase(String artistName);



}

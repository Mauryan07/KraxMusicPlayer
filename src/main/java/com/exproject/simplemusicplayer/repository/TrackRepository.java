package com.exproject.simplemusicplayer.repository;

import com.exproject.simplemusicplayer.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    // Search by artist name (artist is mandatory so INNER JOIN)
//List<Track> findByArtistNameContainingIgnoreCase(@Param("artistName") String artistName);

    // Search by album title with LEFT JOIN to include null/unknown albums
 //   List<Track> findByAlbumTitleContainingIgnoreCase(@Param("albumTitle") String albumTitle);

    // Search by title (no joins needed)
    List<Track> findByTitleContainingIgnoreCase(String title);

    Optional<Track> findByTitleIgnoreCase(String title);

    // Unified keyword search with LEFT JOIN on album
 //   List<Track> searchByKeyword(@Param("keyword") String keyword);
}

package com.exproject.simplemusicplayer.repository;

import com.exproject.simplemusicplayer.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {



    List<Track> findByTitleContainingIgnoreCase(String title);

    Optional<Track> findByTitleIgnoreCase(String title);


}

package com.exproject.kraxmusicplayer.service;

import com.exproject.kraxmusicplayer.dto.TrackDTO;
import com.exproject.kraxmusicplayer.service.impl.TrackServiceImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TrackService {
    long getTrackCount();

    List<TrackDTO> searchByTitle(String title);

    List<TrackDTO> listAllSongs();

    TrackServiceImpl.trackWithArtwork getTrackByFileHash(long fileHash);

    void deleteTrackByFileHash(Long fileHash);

    List<TrackDTO> listAllTracksInPages(Pageable pageable);

    boolean deleteAllTracks();

    String getTrackFilePath(long hash);
}

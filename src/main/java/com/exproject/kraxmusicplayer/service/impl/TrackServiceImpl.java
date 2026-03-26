package com.exproject.kraxmusicplayer.service.impl;

import com.exproject.kraxmusicplayer.dto.TrackDTO;
import com.exproject.kraxmusicplayer.dto.TrackWithArtworkDTO;
import com.exproject.kraxmusicplayer.model.Album;
import com.exproject.kraxmusicplayer.model.Track;
import com.exproject.kraxmusicplayer.repository.AlbumRepository;
import com.exproject.kraxmusicplayer.repository.ArtistRepository;
import com.exproject.kraxmusicplayer.repository.ArtworkRepository;
import com.exproject.kraxmusicplayer.repository.TrackRepository;
import com.exproject.kraxmusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackServiceImpl implements TrackService {

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;

    public record trackWithArtwork(TrackWithArtworkDTO trackInfo, byte[] trackFile) {}

    private TrackDTO toDTO(Track track) {
        return new TrackDTO(
                track.getFileHash(),
                track.getTitle(),
                track.getDuration(),
                track.getBitrate(),
                track.getFilePath()
        );
    }

    private TrackWithArtworkDTO toTWADTO(Track track) {
        byte[] imgData = null;
        String mime = null;
        if (track.getAlbum() != null && track.getAlbum().getArtwork() != null) {
            imgData = track.getAlbum().getArtwork().getImageData();
            mime = track.getAlbum().getArtwork().getMimeType();
        }

        return new TrackWithArtworkDTO(
                track.getFileHash(),
                track.getTitle(),
                track.getDuration(),
                track.getBitrate(),
                track.getFilePath(), // now playlist path
                imgData,
                mime
        );
    }

    @Override
    public long getTrackCount() {
        return trackRepository.count();
    }

    @Override
    public List<TrackDTO> searchByTitle(String title) {
        return trackRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrackDTO> listAllSongs() {
        return trackRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public trackWithArtwork getTrackByFileHash(long fileHash) {
        TrackWithArtworkDTO trackInfo = trackRepository.findById(fileHash)
                .map(this::toTWADTO)
                .orElse(null);

        // HLS: do not load full audio bytes; player will fetch segments
        return new trackWithArtwork(trackInfo, null);
    }

    @Override
    public List<TrackDTO> listAllTracksInPages(Pageable pageable) {
        return trackRepository.findAll(pageable).stream().map(this::toDTO).toList();
    }

    // DELETE LOGIC (unchanged)

    @Override
    @Transactional
    public void deleteTrackByFileHash(Long fileHash) {
        trackRepository.findById(fileHash).ifPresent(track -> {
            String playlistPath = track.getFilePath(); // points to playlist.m3u8
            Album album = track.getAlbum();

            // Delete HLS folder (playlist + segments)
            if (playlistPath != null) {
                Path playlist = Paths.get(playlistPath);
                Path dir = playlist.getParent();
                try {
                    if (dir != null && Files.exists(dir)) {
                        // delete all files in the HLS directory
                        Files.list(dir).forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
                        Files.deleteIfExists(dir);
                    } else {
                        Files.deleteIfExists(playlist); // fallback
                    }
                } catch (IOException e) {
                    System.err.println("Error deleting HLS files: " + e.getMessage());
                }
            }

            // Remove track from DB
            trackRepository.delete(track);

            // Clean up album if empty
            if (album != null) {
                List<Track> remaining = album.getTracks();
                if (remaining == null || remaining.isEmpty() || (remaining.contains(track) && remaining.size() == 1)) {
                    if (album.getArtwork() != null) {
                        artworkRepository.delete(album.getArtwork());
                    }
                    albumRepository.delete(album);
                }
            }
        });
    }

    @Override
    @Transactional
    public boolean deleteAllTracks() {
        try {
            // Delete all HLS folders/files
            for (Track track : trackRepository.findAll()) {
                String playlistPath = track.getFilePath();
                if (playlistPath != null) {
                    Path playlist = Paths.get(playlistPath);
                    Path dir = playlist.getParent();
                    try {
                        if (dir != null && Files.exists(dir)) {
                            Files.list(dir).forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                            });
                            Files.deleteIfExists(dir);
                        } else {
                            Files.deleteIfExists(playlist);
                        }
                    } catch (IOException ignored) {}
                }
            }

            trackRepository.deleteAllInBatch();
            artworkRepository.deleteAllInBatch();
            albumRepository.deleteAllInBatch();
            artistRepository.deleteAllInBatch();

            return trackRepository.count() == 0 && albumRepository.count() == 0;
        } catch (Exception e) {
            System.err.println("Error during deleteAllTracks: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getTrackFilePath(long fileHash) {
        return trackRepository.findByFileHash(fileHash)
                .map(Track::getFilePath)
                .orElse(null);
    }
}
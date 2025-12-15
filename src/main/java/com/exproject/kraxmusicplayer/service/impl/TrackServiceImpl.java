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
import org.springframework.cache.annotation.Cacheable;
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

    public record trackWithArtwork(TrackWithArtworkDTO trackInfo, byte[] trackFile) {
    }


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
        return new TrackWithArtworkDTO(
                track.getFileHash(),
                track.getTitle(),
                track.getDuration(),
                track.getBitrate(),
                track.getFilePath(),
                track.getAlbum().getArtwork().getImageData(),
                track.getAlbum().getArtwork().getMimeType()
        );
    }


    @Override
    @Cacheable("trackCount")
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

    public trackWithArtwork getTrackByFileHash(long fileHash) {
        TrackWithArtworkDTO trackInfo = trackRepository.findById(fileHash).stream().map(this::toTWADTO).findFirst().orElse(null);
        //getting file data from server file system
        byte[] trackFile = null;
        if (trackInfo != null) {
            try {
                Path filePath = Paths.get(trackInfo.getFilePath());
                if (!Files.exists(filePath)) {
                    System.out.printf("Audio file not found at: %s", filePath);
                    throw new RuntimeException("Audio file not found at: " + filePath);
                }
                trackFile = Files.readAllBytes(filePath);


            } catch (IOException e) {
                System.out.printf("Failed to read audio file for track: %s, error: %s%n", fileHash, e.getMessage());
                throw new RuntimeException("Failed to read audio file for track: " + fileHash, e);
            }
        }

        return new trackWithArtwork(trackInfo, trackFile);
    }


    @Override
    @Cacheable("tracks")
    public List<TrackDTO> listAllTracksInPages(Pageable pageable) {
        return trackRepository.findAll(pageable).stream().map(this::toDTO).toList();
    }

    //Delete logic

    @Override
    @Transactional
    public void deleteTrackByFileHash(Long fileHash) {
        Optional<Track> trackOpt = trackRepository.findById(fileHash);
        if (trackOpt.isPresent()) {
            Track track = trackOpt.get();
            String trackFilePath = track.getFilePath();
            Album album = track.getAlbum();

            // Delete the track file from the file system
            try {
                if (trackFilePath != null) {
                    Files.deleteIfExists(Paths.get(trackFilePath));
                }
            } catch (IOException e) {
                // Log and decide if you want to fail or proceed
                System.err.println("Error deleting file from filesystem: " + trackFilePath);
            }

            // Remove track from DB
            trackRepository.deleteById(fileHash);

            // Check if album now has 0 tracks. If so, delete the album and its artwork.
            if (album != null) {
                List<Track> remainingTracks = album.getTracks();
                if (remainingTracks == null || remainingTracks.size() <= 1) { // this track is not yet gone from list
                    // Delete artwork file (if artwork is headed by a path or needs removal from DB)
                    if (album.getArtwork() != null) {
                        // If artwork has a filepath, delete file:
                        // String artworkPath = album.getArtwork().getFilePath();
                        // if (artworkPath != null) Files.deleteIfExists(Paths.get(artworkPath));
                        artworkRepository.delete(album.getArtwork());
                    }
                    // Delete album from DB
                    albumRepository.delete(album);
                }
            }
        }
    }


    @Override
    @Transactional
    public boolean deleteAllTracks() {
        // Delete all tracks first (removes tracks referencing albums/artists)
        trackRepository.deleteAllInBatch();

        // Delete all artwork before albums
        artworkRepository.deleteAllInBatch();

        // Now albums are no longer referenced, so you can safely delete
        albumRepository.deleteAllInBatch();
        artistRepository.deleteAllInBatch();

        // Optionally: delete files from the file system here as well!

        return trackRepository.count() == 0
                && albumRepository.count() == 0
                && artistRepository.count() == 0
                && artworkRepository.count() == 0;
    }

    @Override
    public String getTrackFilePath(long fileHash) {
        return trackRepository.findByFileHash(fileHash)
                .map(Track::getFilePath)
                .orElse(null);
    }


}

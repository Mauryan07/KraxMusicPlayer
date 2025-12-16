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
import org.springframework.cache.annotation.Cacheable; // Ensure you have this import if using caching
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
        // Handle potential null album/artwork safely
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
                track.getFilePath(),
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

        // Getting file data from server file system
        byte[] trackFile = null;
        if (trackInfo != null) {
            try {
                Path filePath = Paths.get(trackInfo.getFilePath());
                if (!Files.exists(filePath)) {
                    System.out.printf("Audio file not found at: %s%n", filePath);
                    // Depending on requirements, you might want to return null or throw exception
                    // throw new RuntimeException("Audio file not found at: " + filePath);
                } else {
                    trackFile = Files.readAllBytes(filePath);
                }
            } catch (IOException e) {
                System.out.printf("Failed to read audio file for track: %s, error: %s%n", fileHash, e.getMessage());
                throw new RuntimeException("Failed to read audio file for track: " + fileHash, e);
            }
        }

        return new trackWithArtwork(trackInfo, trackFile);
    }

    @Override
    public List<TrackDTO> listAllTracksInPages(Pageable pageable) {
        return trackRepository.findAll(pageable).stream().map(this::toDTO).toList();
    }

    // ---------------------------------------------------------
    // DELETE LOGIC
    // ---------------------------------------------------------

    @Override
    @Transactional
    public void deleteTrackByFileHash(Long fileHash) {
        Optional<Track> trackOpt = trackRepository.findById(fileHash);
        if (trackOpt.isPresent()) {
            Track track = trackOpt.get();
            String trackFilePath = track.getFilePath();
            Album album = track.getAlbum();

            // 1. Delete the track file from the file system FIRST
            if (trackFilePath != null) {
                try {
                    Path path = Paths.get(trackFilePath);
                    boolean deleted = Files.deleteIfExists(path);
                    if (deleted) {
                        System.out.println("Deleted file: " + trackFilePath);
                    } else {
                        System.out.println("File not found (could not delete): " + trackFilePath);
                    }
                } catch (IOException e) {
                    System.err.println("Error deleting file from filesystem: " + trackFilePath + " - " + e.getMessage());
                    // Decide if you want to abort DB delete if file delete fails.
                    // Usually, we proceed so the DB doesn't point to a phantom file,
                    // or we throw to rollback if strict consistency is needed.
                }
            }

            // 2. Remove track from DB
            trackRepository.delete(track);
            // Flushing to ensure the track is removed from the persistence context
            // before checking album contents might be necessary depending on JPA config,
            // but usually the collection check below works on the in-memory entity state.

            // 3. Clean up Album if it has no more tracks
            if (album != null) {
                // The current track is likely still in the list in memory if not explicitly removed,
                // so we check if size <= 1 (meaning this was the last one).
                List<Track> remainingTracks = album.getTracks();

                if (remainingTracks == null || remainingTracks.isEmpty() || (remainingTracks.contains(track) && remainingTracks.size() == 1)) {
                    // Delete associated artwork from DB (Artwork is stored as BLOB, not file)
                    if (album.getArtwork() != null) {
                        artworkRepository.delete(album.getArtwork());
                    }
                    // Delete album from DB
                    albumRepository.delete(album);
                    System.out.println("Deleted empty album: " + album.getName());
                }
            }
        }
    }

    @Override
    @Transactional
    public boolean deleteAllTracks() {
        try {
            // 1. Retrieve all tracks to get file paths
            List<Track> allTracks = trackRepository.findAll();

            // 2. Delete all physical files from storage
            for (Track track : allTracks) {
                String filePath = track.getFilePath();
                if (filePath != null) {
                    try {
                        Files.deleteIfExists(Paths.get(filePath));
                        System.out.println("Deleted file: " + filePath);
                    } catch (IOException e) {
                        System.err.println("Failed to delete file: " + filePath);
                    }
                }
            }

            // 3. Delete from Database
            // Delete tracks
            trackRepository.deleteAllInBatch();

            artworkRepository.deleteAllInBatch();

            albumRepository.deleteAllInBatch();


            artistRepository.deleteAllInBatch();

            return trackRepository.count() == 0
                    && albumRepository.count() == 0;

        } catch (Exception e) {
            System.err.println("Error during deleteAllTracks: " + e.getMessage());
            e.printStackTrace();
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
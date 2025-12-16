package com.exproject.kraxmusicplayer.service.impl;

import com.exproject.kraxmusicplayer.model.Album;
import com.exproject.kraxmusicplayer.model.Artist;
import com.exproject.kraxmusicplayer.model.Martwork;
import com.exproject.kraxmusicplayer.model.Track;
import com.exproject.kraxmusicplayer.repository.AlbumRepository;
import com.exproject.kraxmusicplayer.repository.ArtistRepository;
import com.exproject.kraxmusicplayer.repository.ArtworkRepository;
import com.exproject.kraxmusicplayer.repository.TrackRepository;
import com.exproject.kraxmusicplayer.service.StorageService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    @Value("${track.storage.location}")
    private String storagePath;

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".mp3", ".m4a");
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/m4a", "audio/x-m4a", "audio/mp4", "audio/aac", "video/mp4"
    );

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(storagePath));
    }

    @Override
    @Transactional
    public void store(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for upload");
        }

        for (MultipartFile file : files) {
            try {
                if (file.isEmpty() || file.getOriginalFilename() == null) continue;
                if (!isSupported(file)) {
                    System.out.println("Skipping unsupported file type: " + file.getOriginalFilename());
                    continue;
                }

                String originalFileName = file.getOriginalFilename();
                Path destination = Paths.get(storagePath, originalFileName);
                Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

                // Reuse the processing logic
                processAudioFile(destination);

            } catch (Exception e) {
                System.err.println("Failed to store file: " + file.getOriginalFilename() + " - " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void processTrack(Path path) {
        // Wrapper for the scanner to call
        processAudioFile(path);
    }

    /**
     * Core logic to read metadata and save entities to DB.
     */
    private void processAudioFile(Path path) {
        try {
            String fileName = path.getFileName().toString();
            AudioFile audioFile = AudioFileIO.read(path.toFile());

            if (audioFile == null) return;

            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            if (audioHeader == null) return;

            // Extract metadata
            String title = getFileNameWithoutExtension(fileName);
            String duration = formatDuration(audioHeader.getTrackLength());
            String bitrate = audioHeader.getBitRate();
            long hashCode = generateHashCode(fileName, audioHeader);

            // Check if track already exists
            if (trackRepository.findByFileHash(hashCode).isPresent()) {
                // System.out.println("Track already exists: " + fileName);
                return;
            }

            // Extract Artist
            String artistName = "Unknown Artist";
            if (tag != null) {
                String rawArtistName = tag.getFirst(FieldKey.ARTIST);
                if (rawArtistName != null && !rawArtistName.isBlank()) {
                    artistName = rawArtistName;
                }
            }
            Artist artist = artistRepository.findByNameIgnoreCase(artistName).orElse(null);
            if (artist == null) {
                artist = artistRepository.save(Artist.builder().name(artistName).build());
            }

            // Extract Album
            String albumName = "Unknown Album";
            if (tag != null) {
                String rawAlbumName = tag.getFirst(FieldKey.ALBUM);
                if (rawAlbumName != null && !rawAlbumName.isBlank()) {
                    albumName = rawAlbumName;
                }
            }
            Album album = albumRepository.findByNameIgnoreCase(albumName).orElse(null);
            if (album == null) {
                album = albumRepository.save(Album.builder().name(albumName).build());
            }

            // Extract Artwork
            if (album.getArtwork() == null && tag != null) {
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    try {
                        Martwork newArtwork = Martwork.builder()
                                .album(album)
                                .imageData(artwork.getBinaryData())
                                .mimeType(artwork.getMimeType())
                                .build();
                        artworkRepository.save(newArtwork);
                        album.setArtwork(newArtwork);
                        albumRepository.save(album);
                    } catch (Exception e) {
                        System.err.println("Error extracting artwork: " + e.getMessage());
                    }
                }
            }

            // Save Track
            Track track = Track.builder()
                    .fileHash(hashCode)
                    .title(title)
                    .duration(duration)
                    .bitrate(bitrate)
                    .filePath(path.toAbsolutePath().toString())
                    .artist(artist)
                    .album(album)
                    .build();

            trackRepository.save(track);
            System.out.println("Processed track: " + title);

        } catch (Exception e) {
            System.err.println("Error processing file " + path + ": " + e.getMessage());
        }
    }

    // Helper methods (Keep your existing helper methods below)
    private boolean isSupported(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (originalFileName != null) {
            String extension = getFileExtension(originalFileName).toLowerCase();
            if (SUPPORTED_EXTENSIONS.contains(extension)) return true;
        }
        return contentType != null && SUPPORTED_MIME_TYPES.contains(contentType.toLowerCase());
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return fileName;
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private long generateHashCode(String fileName, AudioHeader audioHeader) {
        String hashInput = fileName + audioHeader.getTrackLength() + audioHeader.getBitRate();
        return hashInput.hashCode();
    }
}
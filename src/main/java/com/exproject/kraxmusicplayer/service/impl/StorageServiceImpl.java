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

    // Supported file extensions
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(". mp3", ".m4a");

    // Supported MIME types
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/m4a",
            "audio/x-m4a",
            "audio/mp4",
            "audio/aac",
            "video/mp4"  // M4A files sometimes have this MIME type
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
                // Validate file
                if (file.isEmpty()) {
                    System.out.println("Skipping empty file");
                    continue;
                }

                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null) {
                    System.out.println("Skipping file with null name");
                    continue;
                }

                // Check if file type is supported
                if (!isSupported(file)) {
                    System.out.println("Skipping unsupported file type: " + originalFileName);
                    continue;
                }

                // Save file to storage
                Path destination = Paths.get(storagePath, originalFileName);
                Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

                // Get file name without extension
                String fileNameWithoutExtension = getFileNameWithoutExtension(originalFileName);

                // Read metadata using jaudiotagger (supports both MP3 and M4A)
                AudioFile audioFile = AudioFileIO.read(destination.toFile());

                if (audioFile == null) {
                    System.out.println("Could not read audio file: " + originalFileName);
                    continue;
                }

                AudioHeader audioHeader = audioFile.getAudioHeader();
                Tag tag = audioFile.getTag();

                if (audioHeader == null) {
                    System.out.println("No audio header found for:  " + originalFileName);
                    continue;
                }

                // Extract metadata
                String title = fileNameWithoutExtension;
                String duration = formatDuration(audioHeader.getTrackLength());
                String bitrate = audioHeader.getBitRate();
                long hashCode = generateHashCode(originalFileName, audioHeader);

                // Extract artist
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


                // Extract album
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
                // Extract artwork
                byte[] imageData = null;
                String mimeType = null;

                if (tag != null) {
                    Artwork artwork = tag.getFirstArtwork();
                    if (artwork != null) {
                        try {
                            imageData = artwork.getBinaryData();
                            mimeType = artwork.getMimeType();
                            System.out.println("Artwork extracted, MIME type: " + mimeType);
                        } catch (Exception e) {
                            System.out.println("Error extracting artwork: " + e.getMessage());
                        }
                    } else {
                        System.out.println("No artwork found for file: " + originalFileName);
                    }
                }

                // Save or update artwork
                if (imageData != null && mimeType != null) {
                    Martwork existingArtwork = album.getArtwork();
                    if (existingArtwork == null) {
                        Martwork newArtwork = Martwork.builder()
                                .album(album)
                                .imageData(imageData)
                                .mimeType(mimeType)
                                .build();
                        artworkRepository.save(newArtwork);
                        album.setArtwork(newArtwork);
                        albumRepository.save(album);
                    } else if (existingArtwork.getImageData() == null) {
                        existingArtwork.setImageData(imageData);
                        existingArtwork.setMimeType(mimeType);
                        artworkRepository.save(existingArtwork);
                    }
                }

                // Check if track already exists
                boolean trackExists = trackRepository.findByFileHash(hashCode).isPresent();
                if (trackExists) {
                    System.out.println("Track already exists, skipping:  " + title);
                    continue;
                }

                // Save track
                Track track = Track.builder()
                        .fileHash(hashCode)
                        .title(title)
                        .duration(duration)
                        .bitrate(bitrate)
                        .filePath(destination.toAbsolutePath().toString())
                        .artist(artist)
                        .album(album)
                        .build();

                trackRepository.save(track);

                System.out.println("Saved track - Title: " + title +
                        ", Duration: " + duration +
                        ", Artist: " + artist.getName() +
                        ", Album: " + album.getName() +
                        ", Bitrate: " + bitrate +
                        ", Format: " + getFileExtension(originalFileName).toUpperCase());

            } catch (Exception e) {
                System.err.println("Failed to store file: " + file.getOriginalFilename() + " - " + e.getMessage());
                e.printStackTrace();
                // Continue processing other files instead of throwing exception
            }
        }
    }

    /**
     * Check if the file type is supported (MP3 or M4A)
     */
    private boolean isSupported(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        // Check by extension
        if (originalFileName != null) {
            String extension = getFileExtension(originalFileName).toLowerCase();
            if (SUPPORTED_EXTENSIONS.contains(extension)) {
                return true;
            }
        }

        // Check by MIME type
        return contentType != null && SUPPORTED_MIME_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Get file extension including the dot (e.g., ". mp3")
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    /**
     * Get file name without extension
     */
    private String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    /**
     * Format duration from seconds to MM:SS format
     */
    private String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Generate a hash code for the track
     */
    private long generateHashCode(String fileName, AudioHeader audioHeader) {
        String hashInput = fileName + audioHeader.getTrackLength() + audioHeader.getBitRate();
        return hashInput.hashCode();
    }
}
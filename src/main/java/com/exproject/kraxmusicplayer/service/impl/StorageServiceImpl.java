package com.exproject.kraxmusicplayer.service.impl;

import com.exproject.kraxmusicplayer.model.Album;
import com.exproject.kraxmusicplayer.model.Artist;
import com.exproject.kraxmusicplayer.model.Martwork;
import com.exproject.kraxmusicplayer.model.Track;
import com.exproject.kraxmusicplayer.repository.AlbumRepository;
import com.exproject.kraxmusicplayer.repository.ArtistRepository;
import com.exproject.kraxmusicplayer.repository.ArtworkRepository;
import com.exproject.kraxmusicplayer.repository.TrackRepository;
import com.exproject.kraxmusicplayer.service.HlsTranscodeService;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    @Value("${track.storage.location}")
    private String storagePath;

    @Value("${ffmpeg.timeoutSeconds:300}")
    private int timeoutSeconds;

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;
    private final HlsTranscodeService hlsTranscodeService;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".mp3", ".m4a");
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/m4a", "audio/x-m4a", "audio/mp4", "audio/aac", "video/mp4"
    );

    // Executor to avoid blocking HTTP threads during ffmpeg
    private final ExecutorService hlsExecutor = Executors.newSingleThreadExecutor();

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

                // Process + transcode (synchronous with timeout to keep response bounded)
                processTrack(destination);

            } catch (Exception e) {
                System.err.println("Failed to store file: " + file.getOriginalFilename() + " - " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public Path processTrack(Path path) {
        return processAudioFile(path);
    }

    /**
     * Core logic to read metadata, save entities to DB, transcode to HLS, and delete source.
     */
    private Path processAudioFile(Path path) {
        try {
            String fileName = path.getFileName().toString();
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            if (audioFile == null) return null;

            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();
            if (audioHeader == null) return null;

            // Metadata
            String title = tag.getFirst(FieldKey.TITLE); //getFileNameWithoutExtension(fileName);
            String duration = formatDuration(audioHeader.getTrackLength());
            String rawBitrate = audioHeader.getBitRate();
            String bitrate = rawBitrate.replaceAll("[^0-9]", "");
            String rawYear = tag.getFirst(FieldKey.YEAR);
            String year = null;
            if (rawYear != null) year = "Unknown";
            if (rawYear != null && rawYear.length() >= 4) {
                year = rawYear.substring(0, 4);
            }
//            System.out.println("year: " + year);
            long hashCode = Math.abs(generateHashCode(fileName, audioHeader));

            if (trackRepository.findByFileHash(hashCode).isPresent()) {
                return null; // already stored
            }

            // Artist
            String artistName;
            if (tag != null) {
                String rawArtistName = tag.getFirst(FieldKey.ARTIST);
                if (rawArtistName != null && !rawArtistName.isBlank()) artistName = rawArtistName;
                else {
                    artistName = "Unknown Artist";
                }
            } else {
                artistName = "Unknown Artist";
            }
            Artist artist = artistRepository.findByNameIgnoreCase(artistName).orElseGet(() ->
                    artistRepository.save(Artist.builder().name(artistName).build()));

            // Album
            String albumName;
            if (tag != null) {
                String rawAlbumName = tag.getFirst(FieldKey.ALBUM);
                if (rawAlbumName != null && !rawAlbumName.isBlank()) albumName = rawAlbumName;
                else {
                    albumName = "Unknown Album";
                }
            } else {
                albumName = "Unknown Album";
            }
            Album album = albumRepository.findByNameIgnoreCase(albumName).orElseGet(() ->
                    albumRepository.save(Album.builder().name(albumName).build()));

            // Artwork
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

            // HLS target dir using title slug
            String titleSlug = title.replaceAll("[^a-zA-Z0-9-_]+", "_");
            Path targetDir = Paths.get(storagePath, titleSlug);

            // Transcode to HLS asynchronously but wait with timeout to keep request bounded
            Future<Path> fut = hlsExecutor.submit(() -> hlsTranscodeService.transcodeToHls(path, targetDir, bitrate));
            Path playlistPath;
            try {
                playlistPath = fut.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                fut.cancel(true);
                throw new RuntimeException("FFmpeg timed out after " + timeoutSeconds + "s", te);
            }

            // Delete original source file after successful HLS generation
            Files.deleteIfExists(path);

            // Save Track with playlist path
            Track track = Track.builder()
                    .fileHash(hashCode)
                    .title(title)
                    .duration(duration)
                    .bitrate(bitrate)
                    .filePath(playlistPath.toAbsolutePath().toString()) // store playlist
                    .artist(artist)
                    .album(album)
                    .year(year)
                    .build();

            trackRepository.save(track);
            System.out.println("Processed track (HLS): " + title);
            return playlistPath;

        } catch (Exception e) {
            System.err.println("Error processing file " + path + ": " + e.getMessage());
            return null;
        }
    }

    // Helpers
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
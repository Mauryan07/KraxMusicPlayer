package com.exproject.kraxmusicplayer.controller;

import com.exproject.kraxmusicplayer.dto.AlbumDTO;
import com.exproject.kraxmusicplayer.dto.TrackDTO;
import com.exproject.kraxmusicplayer.responseMessage.ResponseMessage;
import com.exproject.kraxmusicplayer.service.AlbumService;
import com.exproject.kraxmusicplayer.service.TrackService;
import com.exproject.kraxmusicplayer.service.impl.TrackServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrackController {

    private final TrackService trackService;
    private final AlbumService albumService;

    @Value("${track.storage.location}")
    private String storagePath;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".mp3", ".m4a");

    // Listing all in pages and sorting
    @GetMapping("/tracks")
    public List<TrackDTO> listAllTracksPaged(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "title") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(
                sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return trackService.listAllTracksInPages(pageable);
    }

    // List all songs using DTO
    @GetMapping("/listTracks")
    public List<TrackDTO> listAllTracks() {
        return trackService.listAllSongs();
    }

    @GetMapping("/albums")
    public List<AlbumDTO> listAllAlbums() {
        return albumService.listAllAlbums();
    }

    @GetMapping("/album/{albumName}")
    public AlbumDTO getAlbum(@PathVariable("albumName") String name) {

        return albumService.getAlbumByName(name);
    }


    // Sending specific track audio
    @GetMapping("/track/{fileHash}/audio")
    public void streamTrackAudio(@PathVariable String fileHash, HttpServletResponse response) {
        try {
            long hash = Long.parseLong(fileHash);

            TrackServiceImpl.trackWithArtwork track = trackService.getTrackByFileHash(hash);
            if (track == null || track.trackFile() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Determine content type based on file extension
            String filePath = trackService.getTrackFilePath(hash);
            String contentType = "audio/mpeg"; // Default to MP3

            if (filePath != null) {
                if (filePath.toLowerCase().endsWith(".m4a")) {
                    contentType = "audio/mp4";
                } else if (filePath.toLowerCase().endsWith(".mp3")) {
                    contentType = "audio/mpeg";
                }
            }

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=\"track\"");
            response.setContentLength(track.trackFile().length);

            response.getOutputStream().write(track.trackFile());
            response.getOutputStream().flush();
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Sending specific track artwork
    @GetMapping("/track/{fileHash}/artwork")
    public void streamTrackArtwork(@PathVariable String fileHash, HttpServletResponse response) {
        try {
            long hash = Long.parseLong(fileHash);

            TrackServiceImpl.trackWithArtwork track = trackService.getTrackByFileHash(hash);
            if (track == null || track.trackInfo() == null || track.trackInfo().getImageData() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType(track.trackInfo().getMimeType());
            response.setHeader("Content-Disposition", "inline; filename=\"artwork. jpg\"");
            response.setContentLength(track.trackInfo().getImageData().length);

            response.getOutputStream().write(track.trackInfo().getImageData());
            response.getOutputStream().flush();
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Scan library for new files
    @PostMapping("/track/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> scanLibrary() {
        try {
            Path storageDir = Paths.get(storagePath);

            if (!Files.exists(storageDir)) {
                return ResponseEntity.badRequest()
                        .body(ResponseMessage.builder()
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .message("Storage directory does not exist")
                                .build());
            }

            // Use AtomicInteger for thread-safe counting in lambda
            AtomicInteger scannedCount = new AtomicInteger(0);

            try (Stream<Path> paths = Files.walk(storageDir)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isSupportedFile)
                        .forEach(path -> {
                            scannedCount.incrementAndGet();
                            // Here you could add logic to check if file exists in DB
                            // and add it if not
                        });
            }

            return ResponseEntity.ok(ResponseMessage.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Scan completed.  Found " + scannedCount.get() + " audio files.")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Scan failed: " + e.getMessage())
                            .build());
        }
    }

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    // Delete APIs
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/track/{fileHash}")
    public ResponseEntity<ResponseMessage> deleteTrack(@PathVariable String fileHash) {
        try {
            if (fileHash != null) {
                trackService.deleteTrackByFileHash(Long.parseLong(fileHash));
                return ResponseEntity.ok(ResponseMessage.builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Track deleted successfully")
                        .build());
            }
            return ResponseEntity.badRequest()
                    .body(ResponseMessage.builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message("Invalid file hash")
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Delete failed: " + e.getMessage())
                            .build());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/track/deleteAll")
    public ResponseEntity<ResponseMessage> deleteAllTracks() {
        boolean status = trackService.deleteAllTracks();

        if (status) {
            return ResponseEntity.ok(ResponseMessage.builder()
                    .statusCode(HttpServletResponse.SC_ACCEPTED)
                    .message("Deleted all songs from library!")
                    .build());
        }

        return ResponseEntity.status(HttpServletResponse.SC_EXPECTATION_FAILED)
                .body(ResponseMessage.builder()
                        .statusCode(HttpServletResponse.SC_EXPECTATION_FAILED)
                        .message("Deletion failed or library is empty")
                        .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/album/{albumId}")
    public ResponseEntity<ResponseMessage> deleteAlbum(@PathVariable Long albumId) {
        try {
            albumService.deleteAlbum(albumId);
            return ResponseEntity.ok(ResponseMessage.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Album deleted successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Delete failed: " + e.getMessage())
                            .build());
        }
    }
}
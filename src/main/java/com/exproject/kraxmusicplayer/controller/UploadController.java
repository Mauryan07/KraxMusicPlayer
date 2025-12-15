package com.exproject.kraxmusicplayer.controller;

import com.exproject.kraxmusicplayer.responseMessage.ResponseMessage;
import com.exproject.kraxmusicplayer.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UploadController {

    private final StorageService storageService;

    /**
     * Upload multiple audio files (MP3, M4A)
     */
    @PostMapping("/track/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> uploadFiles(@RequestParam("file") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseMessage.builder()
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .message("No files provided")
                                .build());
            }

            storageService.store(files);

            return ResponseEntity.ok(ResponseMessage.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Successfully processed " + files.size() + " file(s)")
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseMessage.builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Upload failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Upload single audio file (MP3, M4A)
     */
    @PostMapping("/track/upload/single")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> uploadSingleFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseMessage.builder()
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .message("No file provided")
                                .build());
            }

            storageService.store(List.of(file));

            return ResponseEntity.ok(ResponseMessage.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Successfully uploaded:  " + file.getOriginalFilename())
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Upload failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Legacy endpoint for backward compatibility
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> uploadFilesLegacy(@RequestParam("files") List<MultipartFile> files) {
        return uploadFiles(files);
    }
}
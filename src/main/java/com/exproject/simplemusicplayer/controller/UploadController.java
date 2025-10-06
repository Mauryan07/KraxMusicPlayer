package com.exproject.simplemusicplayer.controller;

import com.exproject.simplemusicplayer.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173,http://localhost:5174")
public class UploadController {

    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("files") List<MultipartFile> files) {
        storageService.store(files);
        return ResponseEntity.ok("List of Added Files: " + files.stream().map(MultipartFile::getOriginalFilename).collect(Collectors.joining("   ")));
    }
}

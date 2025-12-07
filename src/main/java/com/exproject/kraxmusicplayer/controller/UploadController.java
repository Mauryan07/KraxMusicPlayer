package com.exproject.kraxmusicplayer.controller;

import com.exproject.kraxmusicplayer.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UploadController {

    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("files") List<MultipartFile> files) {
        storageService.store(files);
        return ResponseEntity.ok("List of Added Files: " + (long) files.stream().count());
    }
}

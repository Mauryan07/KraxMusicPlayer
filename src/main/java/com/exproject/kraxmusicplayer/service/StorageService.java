package com.exproject.kraxmusicplayer.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface StorageService {
    void store(List<MultipartFile> file);

    void processTrack(Path path); // New method
}
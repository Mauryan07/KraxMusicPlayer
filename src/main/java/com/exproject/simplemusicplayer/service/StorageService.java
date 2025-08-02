package com.exproject.simplemusicplayer.service;

import com.exproject.simplemusicplayer.dto.ArtworkDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    void store(List<MultipartFile> file);
}

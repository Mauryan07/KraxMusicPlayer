package com.exproject.simplemusicplayer.service;

import com.exproject.simplemusicplayer.model.Album;
import com.exproject.simplemusicplayer.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;

    @Override
    public List<Album> searchAlbums(String album) {
        return albumRepository.findByNameContainingIgnoreCase(album).stream().map(album);
    }
}

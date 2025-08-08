package com.exproject.simplemusicplayer.service;

import com.exproject.simplemusicplayer.dto.AlbumDTO;
import com.exproject.simplemusicplayer.model.Album;
import com.exproject.simplemusicplayer.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;

    private AlbumDTO toDTO(Album album) {
        return new AlbumDTO(
                album.getName(),
                album.getTracks(),
                album.getArtwork()
        );
    }

    @Override
    public List<AlbumDTO> searchAlbums(String album) {
        return albumRepository.findByNameContainingIgnoreCase(album)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlbumDTO> listAllAlbums() {
        return albumRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }





}

package com.exproject.simplemusicplayer.service.impl;

import com.exproject.simplemusicplayer.dto.AlbumDTO;
import com.exproject.simplemusicplayer.dto.ArtworkDTO;
import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.model.Album;
import com.exproject.simplemusicplayer.repository.AlbumRepository;
import com.exproject.simplemusicplayer.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;

    private AlbumDTO toDTO(Album album) {
        // Convert Track entities to TrackDTO objects
        List<TrackDTO> trackDTOs = album.getTracks().stream()
                .map(track -> new TrackDTO(
                        track.getFileHash(),
                        track.getTitle(),
                        track.getDuration(),
                        track.getBitrate(),
                        track.getFilePath()
                ))
                .collect(Collectors.toList());

        // Convert Artwork model to ArtworkDTO
        ArtworkDTO artworkDTO = null;
        if (album.getArtwork() != null) {
            artworkDTO = new ArtworkDTO(
                    album.getArtwork().getImageData(),
                    album.getArtwork().getMimeType()
            );
        }

        return new AlbumDTO(
                album.getName(),
                trackDTOs,
                artworkDTO
        );
    }

    @Override
    public long getAlbumCount() {
        return albumRepository.count();
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

package com.exproject.simplemusicplayer.service.impl;

import com.exproject.simplemusicplayer.dto.AlbumDTO;
import com.exproject.simplemusicplayer.dto.ArtworkDTO;
import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.model.Album;
import com.exproject.simplemusicplayer.model.Track;
import com.exproject.simplemusicplayer.repository.AlbumRepository;
import com.exproject.simplemusicplayer.repository.ArtworkRepository;
import com.exproject.simplemusicplayer.repository.TrackRepository;
import com.exproject.simplemusicplayer.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final ArtworkRepository artworkRepository;


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

    //delete logic

    @Transactional
    public void deleteAlbum(Long albumId) {
        Optional<Album> albumOpt = albumRepository.findById(albumId);
        if (albumOpt.isPresent()) {
            Album album = albumOpt.get();

            // Delete all associated tracks (DB + files)
            for (Track track : album.getTracks()) {
                String trackFilePath = track.getFilePath();
                try {
                    if (trackFilePath != null) {
                        Files.deleteIfExists(Paths.get(trackFilePath));
                    }
                } catch (IOException e) {
                    System.err.println("Error deleting track file: " + trackFilePath);
                }
                trackRepository.delete(track);
            }

            // Delete artwork file and entity if needed
            if (album.getArtwork() != null) {
                // If artwork has file, delete it
                // String artworkPath = album.getArtwork().getFilePath();
                // if (artworkPath != null) Files.deleteIfExists(Paths.get(artworkPath));
                artworkRepository.delete(album.getArtwork());
            }

            // Delete the album itself
            albumRepository.delete(album);
        }
    }




}

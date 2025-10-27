package com.exproject.simplemusicplayer.service.impl;


import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.dto.TrackWithArtworkDTO;
import com.exproject.simplemusicplayer.entity.Track;
import com.exproject.simplemusicplayer.repository.TrackRepository;
import com.exproject.simplemusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackServiceImpl implements TrackService {

    private final TrackRepository trackRepository;

    public record trackWithArtwork(TrackWithArtworkDTO trackInfo, byte[] trackFile) {
    }


    private TrackDTO toDTO(Track track) {
        return new TrackDTO(
                track.getFileHash(),
                track.getTitle(),
                track.getDuration(),
                track.getBitrate(),
                track.getFilePath()
        );
    }

    private TrackWithArtworkDTO toTWADTO(Track track) {
        return new TrackWithArtworkDTO(
                track.getFileHash(),
                track.getTitle(),
                track.getDuration(),
                track.getBitrate(),
                track.getFilePath(),
                track.getAlbum().getArtwork().getImageData(),
                track.getAlbum().getArtwork().getMimeType()
        );
    }


    @Override
    public List<TrackDTO> searchByTitle(String title) {
        return trackRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrackDTO> listAllSongs() {
        return trackRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public trackWithArtwork getTrackByFileHash(long fileHash) {
        TrackWithArtworkDTO trackInfo = trackRepository.findById(fileHash).stream().map(this::toTWADTO).findFirst().orElse(null);
        //getting file data from server file system
        byte[] trackFile = null;
        if (trackInfo != null) {
            try {
                Path filePath = Paths.get(trackInfo.getFilePath());
                if (!Files.exists(filePath)) {
                    System.out.printf("Audio file not found at: %s", filePath);
                    throw new RuntimeException("Audio file not found at: " + filePath);
                }
                trackFile = Files.readAllBytes(filePath);


            } catch (IOException e) {
                System.out.printf("Failed to read audio file for track: %s, error: %s%n", fileHash, e.getMessage());
                throw new RuntimeException("Failed to read audio file for track: " + fileHash, e);
            }
        }

        return new trackWithArtwork(trackInfo, trackFile);
    }

    @Override
    public void deleteTrackByFileHash(Long fileHash) {
        if (trackRepository.findById(fileHash).isPresent())
            trackRepository.deleteById(fileHash);
    }



}

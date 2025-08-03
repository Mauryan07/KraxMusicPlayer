package com.exproject.simplemusicplayer.service.impl;


import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.model.Track;
import com.exproject.simplemusicplayer.repository.TrackRepository;
import com.exproject.simplemusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackServiceImpl implements TrackService {

    private final TrackRepository trackRepository;

    private TrackDTO toDTO(Track track) {
        return new TrackDTO(
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




//    @Override
//    public List<TrackDTO> searchByAlbum(String albumTitle) {
//        return trackRepository.findByAlbumTitleContainingIgnoreCase(albumTitle)
//                .stream()
//                .map(this::toDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<TrackDTO> searchByKeyword(String keyword) {
//        return trackRepository.searchByKeyword(keyword)
//                .stream()
//                .map(this::toDTO)
//                .collect(Collectors.toList());
//    }
}

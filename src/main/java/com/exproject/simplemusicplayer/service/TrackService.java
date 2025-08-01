package com.exproject.simplemusicplayer.service;

import com.exproject.simplemusicplayer.dto.TrackDTO;

import java.util.List;

public interface TrackService {
    List<TrackDTO> searchByTitle(String title);
//    List<TrackDTO> searchByArtist(String artistName);
//    List<TrackDTO> searchByAlbum(String albumTitle);
//    List<TrackDTO> searchByKeyword(String keyword);

}

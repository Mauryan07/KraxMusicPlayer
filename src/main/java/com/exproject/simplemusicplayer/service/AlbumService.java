package com.exproject.simplemusicplayer.service;

import com.exproject.simplemusicplayer.dto.AlbumDTO;
import java.util.List;

public interface AlbumService {
    List<AlbumDTO> searchAlbums(String album);

    List<AlbumDTO> listAllAlbums();
}

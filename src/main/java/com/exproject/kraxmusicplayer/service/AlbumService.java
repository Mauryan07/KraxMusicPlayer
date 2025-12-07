package com.exproject.kraxmusicplayer.service;

import com.exproject.kraxmusicplayer.dto.AlbumDTO;
import java.util.List;

public interface AlbumService {
    long getAlbumCount();

    List<AlbumDTO> searchAlbums(String album);

    List<AlbumDTO> listAllAlbums();

    void deleteAlbum(Long albumId);
}

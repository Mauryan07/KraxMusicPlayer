package com.exproject.simplemusicplayer.service;

import com.exproject.simplemusicplayer.model.Album;

import java.util.List;

public interface AlbumService {
    List<Album> searchAlbums(String album);
}

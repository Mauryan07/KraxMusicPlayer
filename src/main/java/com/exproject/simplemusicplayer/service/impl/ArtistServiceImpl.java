package com.exproject.simplemusicplayer.service.impl;

import com.exproject.simplemusicplayer.repository.ArtistRepository;
import com.exproject.simplemusicplayer.service.ArtistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArtistServiceImpl implements ArtistService {
    @Autowired
    ArtistRepository artistRepository;

    @Override
    public long getArtistCount() {
        return artistRepository.count();
    }
}

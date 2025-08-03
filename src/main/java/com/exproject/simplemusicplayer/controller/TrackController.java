package com.exproject.simplemusicplayer.controller;

import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    @GetMapping
    public List<TrackDTO> listAllSongs() {
        return (trackService.listAllSongs());
    }

    @GetMapping
    public List<TrackDTO> listAllAlbums() {
        return (trackService.listAllSongs());
    }





}

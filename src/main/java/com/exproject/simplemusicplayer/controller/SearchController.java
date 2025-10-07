package com.exproject.simplemusicplayer.controller;


import com.exproject.simplemusicplayer.dto.AlbumDTO;
import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.service.AlbumService;
import com.exproject.simplemusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    private final TrackService trackService;
    private final AlbumService albumService;

    @GetMapping("/title")
    public List<TrackDTO> searchTracks(@RequestParam String name) {
        return (trackService.searchByTitle(name));
    }

    @GetMapping("/album")
    public List<AlbumDTO> searchAlbums(@RequestParam String name) {
        return (albumService.searchAlbums(name));
    }


}

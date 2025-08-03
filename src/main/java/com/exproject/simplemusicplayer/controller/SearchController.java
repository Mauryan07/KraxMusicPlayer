package com.exproject.simplemusicplayer.controller;


import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.model.Album;
import com.exproject.simplemusicplayer.service.AlbumService;
import com.exproject.simplemusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final TrackService trackService;
    private final AlbumService albumService;

    @GetMapping("/title/{title}")
    public List<TrackDTO> searchTracks(@PathVariable String title) {
        return (trackService.searchByTitle(title));
    }

    @GetMapping("/album/{album}")
    public List<Album> searchAlbums(@PathVariable String album) {
        return (albumService.searchAlbums(album));
    }


}

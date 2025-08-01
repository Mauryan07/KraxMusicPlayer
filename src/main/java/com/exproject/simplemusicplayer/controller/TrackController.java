package com.exproject.simplemusicplayer.controller;

import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    @GetMapping("/search")
    public ResponseEntity<List<TrackDTO>> searchTracks(@RequestParam String keyword) {
        return ResponseEntity.ok(trackService.searchByTitle(keyword));
    }


}

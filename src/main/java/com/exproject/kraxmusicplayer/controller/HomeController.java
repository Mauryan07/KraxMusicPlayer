package com.exproject.kraxmusicplayer.controller;

import com.exproject.kraxmusicplayer.dto.*;
import com.exproject.kraxmusicplayer.service.AlbumService;
import com.exproject.kraxmusicplayer.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeController {

    private final TrackService trackService;
    private final AlbumService albumService;

    @GetMapping("/api/home")
    public ResponseEntity<HomeViewDTO> home(
            @RequestParam(value = "tracksLimit", required = false, defaultValue = "12") int tracksLimit,
            @RequestParam(value = "albumsLimit", required = false, defaultValue = "8") int albumsLimit,
            Authentication authentication) {

        // build base url for audio/artwork endpoints
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

        long totalTracks = trackService.getTrackCount();
        long totalAlbums = albumService.getAlbumCount();

        // Recent tracks (page 0)
        List<TrackDTO> trackDtos;
        try {
            Pageable p = PageRequest.of(0, Math.max(1, tracksLimit));
            trackDtos = trackService.listAllTracksInPages(p);
        } catch (Exception ex) {
            trackDtos = trackService.listAllSongs().stream().limit(tracksLimit).collect(Collectors.toList());
        }

        List<HomeTrackDTO> recentTracks = trackDtos.stream()
                .map(t -> toHomeTrackDTO(t, baseUrl))
                .collect(Collectors.toList());

        // Albums
        List<AlbumDTO> albumDtos;
        try {
            albumDtos = albumService.listAllAlbums();
        } catch (Exception ex) {
            albumDtos = Collections.emptyList();
        }

        List<HomeAlbumDTO> albums = albumDtos.stream()
                .limit(albumsLimit)
                .map(a -> toHomeAlbumDTO(a, baseUrl))
                .collect(Collectors.toList());

        // Authenticated user info (if present)
        UserInfoDTO userInfo = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
            userInfo = new UserInfoDTO(username, roles);
        }

        HomeViewDTO view = new HomeViewDTO(
                new CountsDTO(totalTracks, totalAlbums),
                recentTracks,
                albums,
                userInfo
        );

        return ResponseEntity.ok(view);
    }

    // Helper: construct audio + artwork urls for a track
    private HomeTrackDTO toHomeTrackDTO(com.exproject.kraxmusicplayer.dto.TrackDTO t, String baseUrl) {
        String audioUrl = String.format("%s/api/track/%s/audio", baseUrl, t.getFileHash());
        String artworkUrl = String.format("%s/api/track/%s/artwork", baseUrl, t.getFileHash());
        return new HomeTrackDTO(t.getFileHash(), t.getTitle(), t.getDuration(), t.getBitrate(), audioUrl, artworkUrl);
    }

    // Helper: map AlbumDTO -> HomeAlbumDTO. Use first track's fileHash (if exists) for artwork URL.
    private HomeAlbumDTO toHomeAlbumDTO(com.exproject.kraxmusicplayer.dto.AlbumDTO a, String baseUrl) {
        String artworkUrl = null;
        int trackCount = 0;
        List<HomeTrackDTO> sampleTracks = Collections.emptyList();
        if (a.getTracks() != null && !a.getTracks().isEmpty()) {
            trackCount = a.getTracks().size();
            // artwork uses first track's fileHash endpoint
            Long fileHash = a.getTracks().getFirst().getFileHash();
            if (fileHash != null) {
                artworkUrl = String.format("%s/api/track/%s/artwork", baseUrl, fileHash);
            }
            sampleTracks = a.getTracks().stream()
                    .limit(3)
                    .map(t -> new HomeTrackDTO(t.getFileHash(), t.getTitle(), t.getDuration(), t.getBitrate(),
                            String.format("%s/api/track/%s/audio", baseUrl, t.getFileHash()),
                            String.format("%s/api/track/%s/artwork", baseUrl, t.getFileHash())))
                    .collect(Collectors.toList());
        }
        return new HomeAlbumDTO(a.getName(), artworkUrl, trackCount, sampleTracks);
    }
}
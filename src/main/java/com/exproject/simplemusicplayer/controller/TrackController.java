package com.exproject.simplemusicplayer.controller;

import com.exproject.simplemusicplayer.dto.AlbumDTO;
import com.exproject.simplemusicplayer.dto.TrackDTO;
import com.exproject.simplemusicplayer.service.AlbumService;
import com.exproject.simplemusicplayer.service.TrackService;
import com.exproject.simplemusicplayer.service.impl.TrackServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrackController {

    private final TrackService trackService;
    private final AlbumService albumService;

    //listing all
    @GetMapping("/tracks")
    public List<TrackDTO> listAllTracks() {
        return (trackService.listAllSongs());
    }

    @GetMapping("/albums")
    public List<AlbumDTO> listAllAlbums() {
        return (albumService.listAllAlbums());
    }

    //sending specific track
    @GetMapping("/track/{fileHash}/audio")
    public void streamTrackAudio(@PathVariable String fileHash, HttpServletResponse response) {
        try {
            // Parse fileHash to long
            long hash = Long.parseLong(fileHash);

            // Retrieve track data
            TrackServiceImpl.trackWithArtwork track = trackService.getTrackByFileHash(hash);
            if (track == null || track.trackFile() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Set response headers for audio
            response.setContentType("audio/mpeg");
            response.setHeader("Content-Disposition", "inline; filename=\"track.mp3\"");
            response.setContentLength(track.trackFile().length);

            // Write audio data to response
            response.getOutputStream().write(track.trackFile());
            response.getOutputStream().flush();
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    //sending specific track artwork
    @GetMapping("/track/{fileHash}/artwork")
    public void streamTrackArtwork(@PathVariable String fileHash, HttpServletResponse response) {
        try {
            // Parse fileHash to long
            long hash = Long.parseLong(fileHash);

            // Retrieve track data
            TrackServiceImpl.trackWithArtwork track = trackService.getTrackByFileHash(hash);
            if (track == null || track.trackInfo() == null || track.trackInfo().getImageData() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Set response headers for artwork
            response.setContentType(track.trackInfo().getMimeType());
            response.setHeader("Content-Disposition", "inline; filename=\"artwork.jpg\"");
            response.setContentLength(track.trackInfo().getImageData().length);

            // Write artwork data to response
            response.getOutputStream().write(track.trackInfo().getImageData());
            response.getOutputStream().flush();
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    //delete Api's

    @DeleteMapping("/track/{fileHash}")
    public void deleteTrack(@PathVariable String fileHash) {
        if (fileHash != null) trackService.deleteTrackByFileHash(Long.parseLong(fileHash));
    }









}

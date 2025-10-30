package com.exproject.simplemusicplayer.controller;

import com.exproject.simplemusicplayer.dto.HomePageResponseDTO;
import com.exproject.simplemusicplayer.service.AlbumService;
import com.exproject.simplemusicplayer.service.ArtistService;
import com.exproject.simplemusicplayer.service.TrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/", "/home", "/api/home"})
public class HomeController {
    @Autowired
    TrackService trackService;
    @Autowired
    AlbumService albumService;
    @Autowired
    ArtistService artistService;
//    @Autowired
//    UserService userService;


    @GetMapping("")
    public HomePageResponseDTO getHomePage() {

        long trackCount = trackService.getTrackCount();
        long albumCount = albumService.getAlbumCount();
        long artistCount = artistService.getArtistCount();
        String userName = "Nobita";             //UserService.getUserName();
        String welcomeMessage = "Hello !! " + userName;

        return HomePageResponseDTO.builder()
                .trackCount(trackCount)
                .albumCount(albumCount)
                .artistCount(artistCount)
                .userName(userName)
                .welcomeMessage(welcomeMessage)
                .build();
    }

}

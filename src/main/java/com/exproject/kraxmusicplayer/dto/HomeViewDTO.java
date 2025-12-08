package com.exproject.kraxmusicplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeViewDTO {
    private CountsDTO counts;
    private List<HomeTrackDTO> recentTracks;
    private List<HomeAlbumDTO> albums;
    private UserInfoDTO user;
}
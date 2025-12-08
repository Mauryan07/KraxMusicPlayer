package com.exproject.kraxmusicplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeAlbumDTO {
    private String name;
    private String artworkUrl;
    private int trackCount;
    private List<HomeTrackDTO> sampleTracks;
}
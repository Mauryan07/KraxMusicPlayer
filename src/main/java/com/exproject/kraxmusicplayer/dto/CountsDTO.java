package com.exproject.kraxmusicplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountsDTO {
    private long totalTracks;
    private long totalAlbums;
}
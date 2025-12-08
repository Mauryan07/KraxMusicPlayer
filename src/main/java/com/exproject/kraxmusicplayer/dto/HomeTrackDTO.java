package com.exproject.kraxmusicplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight track info used by the home UI (no binary artwork).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeTrackDTO {
    private Long fileHash;
    private String title;
    private String duration;
    private String bitrate;
    private String audioUrl;
    private String artworkUrl;
}
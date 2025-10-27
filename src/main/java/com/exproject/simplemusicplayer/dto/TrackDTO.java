package com.exproject.simplemusicplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackDTO {
    private long fileHash;
    private String title;
    private String duration;
    private String bitrate;
    private String filePath;
}
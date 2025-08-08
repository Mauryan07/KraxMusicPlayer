package com.exproject.simplemusicplayer.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackWithArtworkDTO {
    private String fileHash;
    private String title;
    private String duration;
    private String bitrate;
    private String filePath;
    private byte[] imageData;
    private String mimeType;
}

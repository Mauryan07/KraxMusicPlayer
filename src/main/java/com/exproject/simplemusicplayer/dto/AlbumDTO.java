package com.exproject.simplemusicplayer.dto;

import com.exproject.simplemusicplayer.model.Martwork;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlbumDTO {

    private String name;
    private List<TrackDTO> tracks;
    private Martwork artwork;
}

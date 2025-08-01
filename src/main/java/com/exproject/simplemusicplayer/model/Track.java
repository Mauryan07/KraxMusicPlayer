package com.exproject.simplemusicplayer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {
    @Id
    private String fileHash;
    private String title;
    private String duration;
    private String filePath;
    private String bitrate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Artist artist;


}
